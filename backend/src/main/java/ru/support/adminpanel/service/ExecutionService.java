package ru.support.adminpanel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import ru.support.adminpanel.config.AppProperties;
import ru.support.adminpanel.dto.ExecutionHistoryResponse;
import ru.support.adminpanel.entity.*;
import ru.support.adminpanel.repository.ScriptExecutionRepository;
import ru.support.adminpanel.repository.ScriptRepository;
import ru.support.adminpanel.repository.UserRepository;
import ru.support.adminpanel.security.CurrentUser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Оркестрация запуска скрипта и (по отдельной команде) отправки результата
 * в целевую систему. Логика самой отправки (авторизация, вызов внешнего API)
 * находится внутри загруженных скриптов и backend её не реализует —
 * см. HLD раздел 3.3 "Потоки данных" и раздел 12 (что НЕ нужно реализовывать).
 */
@Service
public class ExecutionService {

    private final ScriptExecutionRepository executionRepository;
    private final ScriptService scriptService;
    private final ScriptRepository scriptRepository;
    private final UserRepository userRepository;
    private final ScriptExecutionEngine engine;
    private final ActionHistoryService actionHistoryService;
    private final AppProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExecutionService(ScriptExecutionRepository executionRepository,
                             ScriptService scriptService,
                             ScriptRepository scriptRepository,
                             UserRepository userRepository,
                             ScriptExecutionEngine engine,
                             ActionHistoryService actionHistoryService,
                             AppProperties props) {
        this.executionRepository = executionRepository;
        this.scriptService = scriptService;
        this.scriptRepository = scriptRepository;
        this.userRepository = userRepository;
        this.engine = engine;
        this.actionHistoryService = actionHistoryService;
        this.props = props;
    }

    public ScriptExecution execute(UUID scriptId, Map<String, String> parameters, CurrentUser actor) {
        ScriptEntity script = scriptService.getOrThrow(scriptId);
        if (!script.isActive()) {
            throw new IllegalStateException("Скрипт деактивирован администратором");
        }

        List<String> orderedValues = orderParameters(script.getParametersConfig(), parameters);

        ScriptExecution execution = new ScriptExecution();
        execution.setScriptId(scriptId);
        execution.setUserId(actor.uuid());
        execution.setParametersJson(toJson(parameters));
        execution.setStatus(ExecutionStatus.RUNNING);
        execution = executionRepository.save(execution);

        Path resultsDir = ensureDir(props.getResultsDir() + "/" + execution.getId());
        ScriptExecutionEngine.Result result = engine.run(script.getScriptType(), script.getFilePath(),
                orderedValues, resultsDir);

        execution.setStdout(truncate(result.stdout));
        execution.setStderr(truncate(result.stderr));
        execution.setFinishedAt(OffsetDateTime.now());

        if (result.exitCode == 0 && !result.timedOut) {
            execution.setStatus(ExecutionStatus.GENERATED);
            // Скрипт создаёт по одному файлу на каждый введённый через запятую счёт
            // (см. цикл foreach($account in $accountArray) в скриптах). Поэтому храним
            // путь к ПАПКЕ результатов выполнения, а не к одному файлу — listResultFiles()
            // ниже перечисляет все файлы внутри при просмотре/скачивании.
            boolean hasFiles = !listFilesInDir(resultsDir).isEmpty();
            execution.setResultFilePath(hasFiles ? resultsDir.toString() : null);
        } else {
            execution.setStatus(ExecutionStatus.FAILED);
        }

        ScriptExecution saved = executionRepository.save(execution);
        actionHistoryService.record(actor.uuid(), "SCRIPT_EXECUTE", "SCRIPT_EXECUTION", saved.getId(),
                "Скрипт: " + script.getName() + ", статус: " + saved.getStatus());
        return saved;
    }

    public ScriptExecution send(UUID executionId, CurrentUser actor) {
        ScriptExecution execution = getOrThrow(executionId);
        if (execution.getStatus() != ExecutionStatus.GENERATED) {
            throw new IllegalStateException("Отправка доступна только для успешно сгенерированных результатов");
        }
        ScriptEntity script = scriptService.getOrThrow(execution.getScriptId());

        if (script.getSendScriptPath() != null && !script.getSendScriptPath().isBlank()) {
            ScriptExecutionEngine.Result result = engine.run(script.getScriptType(), script.getSendScriptPath(),
                    List.of(execution.getResultFilePath() == null ? "" : execution.getResultFilePath()),
                    Path.of(props.getResultsDir() + "/" + execution.getId()));
            if (result.exitCode != 0) {
                execution.setStderr(truncate(result.stderr));
                executionRepository.save(execution);
                throw new IllegalStateException("Ошибка отправки в целевую систему: " + result.stderr);
            }
        }
        // Если отдельный send-скрипт не сконфигурирован, считаем что отправка
        // была выполнена самим генерирующим скриптом (как в реальном примере заказчика),
        // и просто фиксируем факт отправки в аудите (демо-режим MVP).

        execution.setStatus(ExecutionStatus.SENT);
        execution.setSentToTargetAt(OffsetDateTime.now());
        ScriptExecution saved = executionRepository.save(execution);
        actionHistoryService.record(actor.uuid(), "EXECUTION_SEND", "SCRIPT_EXECUTION", executionId, null);
        return saved;
    }

    public ScriptExecution getOrThrow(UUID id) {
        return executionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Выполнение не найдено"));
    }

    public List<ScriptExecution> myExecutions(UUID userId) {
        return executionRepository.findByUserIdOrderByStartedAtDesc(userId);
    }

    /**
     * Общая история всех выполнений (кто/когда/что генерировал) для страницы "История" —
     * см. запрос: файлы результатов должны быть доступны для скачивания из истории,
     * с указанием автора и времени. Обогащает записи выполнений названием скрипта/банка/
     * категории и логином пользователя, чтобы фронту не нужно было делать это самому.
     */
    public List<ExecutionHistoryResponse> allExecutionsWithDetails() {
        List<ScriptExecution> executions = executionRepository.findAllByOrderByStartedAtDesc();

        Map<UUID, ScriptEntity> scriptsById = scriptRepository
                .findAllById(executions.stream().map(ScriptExecution::getScriptId).distinct().toList())
                .stream().collect(Collectors.toMap(ScriptEntity::getId, s -> s));

        Map<UUID, User> usersById = userRepository
                .findAllById(executions.stream().map(ScriptExecution::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        return executions.stream()
                .map(e -> ExecutionHistoryResponse.from(e, scriptsById.get(e.getScriptId()), usersById.get(e.getUserId()),
                        listResultFiles(e)))
                .toList();
    }

    /**
     * Список файлов результата выполнения. Поддерживает оба варианта resultFilePath:
     * новый (путь к папке результатов, там может быть несколько файлов — по одному
     * на счёт) и старый (путь к одному конкретному файлу, для записей, созданных
     * до перехода на множественную генерацию).
     */
    public List<File> listResultFiles(ScriptExecution execution) {
        if (execution.getResultFilePath() == null || execution.getResultFilePath().isBlank()) {
            return List.of();
        }
        File pathFile = new File(execution.getResultFilePath());
        if (pathFile.isDirectory()) {
            return listFilesInDir(pathFile.toPath());
        }
        if (pathFile.isFile()) {
            return List.of(pathFile);
        }
        return List.of();
    }

    /** Упаковывает несколько файлов результата в zip-архив (для скачивания одним файлом). */
    public byte[] zipResultFiles(List<File> files) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (File file : files) {
                zos.putNextEntry(new ZipEntry(file.getName()));
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private List<File> listFilesInDir(Path dir) {
        File[] files = dir.toFile().listFiles(File::isFile);
        if (files == null || files.length == 0) {
            return List.of();
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        return Arrays.asList(files);
    }

    private List<String> orderParameters(String parametersConfigJson, Map<String, String> values) {
        List<String> ordered = new ArrayList<>();
        Map<String, String> safeValues = ScriptExecutionEngine.emptyIfNull(values);
        try {
            JsonNode arr = objectMapper.readTree(parametersConfigJson == null ? "[]" : parametersConfigJson);
            for (JsonNode node : arr) {
                String paramName = node.get("name").asText();
                ordered.add(safeValues.getOrDefault(paramName, ""));
            }
        } catch (IOException e) {
            // Если конфигурация параметров некорректна — передаём значения как есть, без гарантии порядка.
            ordered.addAll(safeValues.values());
        }
        return ordered;
    }

    private Path ensureDir(String path) {
        try {
            Path p = Path.of(path);
            Files.createDirectories(p);
            return p;
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать каталог результатов: " + e.getMessage(), e);
        }
    }

    private String truncate(String s) {
        if (s == null) return null;
        int max = 20000;
        return s.length() > max ? s.substring(0, max) + "...(обрезано)" : s;
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map == null ? Map.of() : map);
        } catch (IOException e) {
            return "{}";
        }
    }
}

package ru.support.adminpanel.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.support.adminpanel.config.AppProperties;
import ru.support.adminpanel.entity.Role;
import ru.support.adminpanel.entity.ScriptEntity;
import ru.support.adminpanel.entity.ScriptType;
import ru.support.adminpanel.repository.ScriptRepository;
import ru.support.adminpanel.security.CurrentUser;
import ru.support.adminpanel.util.SafeFileNames;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Управление скриптами. Загрузка файла скрипта — только роль ADMIN (см. HLD раздел 8:
 * backend выполняет произвольный код с правами сервисной учётной записи, поэтому
 * право добавления скриптов строго ограничено ролью Administrator).
 */
@Service
public class ScriptService {

    private final ScriptRepository scriptRepository;
    private final AppProperties props;
    private final ActionHistoryService actionHistoryService;

    /** Ограничение на размер содержимого при редактировании скрипта прямо в
     *  браузере (см. AdminScriptsPage.jsx) — по аналогии с лимитом на загрузку
     *  файла (application.yml, servlet.multipart.max-file-size), чтобы через
     *  редактор нельзя было записать на диск файл неограниченного размера. */
    private static final int MAX_CONTENT_BYTES = 5 * 1024 * 1024;

    public ScriptService(ScriptRepository scriptRepository, AppProperties props,
                          ActionHistoryService actionHistoryService) {
        this.scriptRepository = scriptRepository;
        this.props = props;
        this.actionHistoryService = actionHistoryService;
    }

    public List<ScriptEntity> listForRole(String role) {
        List<ScriptEntity> all = scriptRepository.findByActiveTrue();
        if ("ADMIN".equals(role)) {
            return all;
        }
        return all.stream()
                .filter(s -> s.getVisibleToRole() == null || s.getVisibleToRole().name().equals(role))
                .toList();
    }

    public ScriptEntity upload(String name, String description, ScriptType type,
                                String parametersConfig, Role visibleToRole,
                                MultipartFile file, CurrentUser actor) {
        return upload(name, description, type, parametersConfig, visibleToRole, null, null, file, actor);
    }

    public ScriptEntity upload(String name, String description, ScriptType type,
                                String parametersConfig, Role visibleToRole,
                                String category, String bankName,
                                MultipartFile file, CurrentUser actor) {
        try {
            Path dir = Path.of(props.getScriptsDir());
            Files.createDirectories(dir);
            // Имя файла обязательно очищаем — file.getOriginalFilename() приходит от клиента
            // без каких-либо гарантий формата, специально сформированное имя с "../" могло бы
            // увести запись за пределы каталога скриптов (path traversal), см. SafeFileNames.
            String safeName = UUID.randomUUID() + "-" + SafeFileNames.sanitize(file.getOriginalFilename());
            Path target = SafeFileNames.resolveInside(dir, safeName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            ScriptEntity s = new ScriptEntity();
            s.setName(name);
            s.setDescription(description);
            s.setFilePath(target.toString());
            s.setScriptType(type);
            s.setParametersConfig(parametersConfig == null ? "[]" : parametersConfig);
            s.setVisibleToRole(visibleToRole);
            s.setCategory(category);
            s.setBankName(bankName);
            s.setUploadedBy(actor.uuid());
            ScriptEntity saved = scriptRepository.save(s);

            actionHistoryService.record(actor.uuid(), "SCRIPT_UPLOAD", "SCRIPT", saved.getId(),
                    "Загружен скрипт " + name + " (" + type + ")");
            return saved;
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить файл скрипта: " + e.getMessage(), e);
        }
    }

    public ScriptEntity toggleActive(UUID id, CurrentUser actor) {
        ScriptEntity s = scriptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Скрипт не найден"));
        s.setActive(!s.isActive());
        ScriptEntity saved = scriptRepository.save(s);
        actionHistoryService.record(actor.uuid(), s.isActive() ? "SCRIPT_ACTIVATE" : "SCRIPT_DEACTIVATE",
                "SCRIPT", id, null);
        return saved;
    }

    public ScriptEntity getOrThrow(UUID id) {
        return scriptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Скрипт не найден"));
    }

    /** Содержимое файла скрипта для редактирования прямо в браузере (/admin/scripts). */
    public String readContent(UUID id) {
        ScriptEntity s = getOrThrow(id);
        try {
            return Files.readString(Path.of(s.getFilePath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось прочитать файл скрипта: " + e.getMessage(), e);
        }
    }

    /** Сохраняет отредактированное содержимое поверх файла скрипта на диске.
     *  ScriptEntity в БД не меняется (путь к файлу остаётся тем же) — только сам файл. */
    public ScriptEntity updateContent(UUID id, String content, CurrentUser actor) {
        ScriptEntity s = getOrThrow(id);
        if (content == null) {
            throw new IllegalArgumentException("Пустое содержимое скрипта");
        }
        if (content.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw new IllegalArgumentException("Содержимое скрипта слишком большое (максимум "
                    + (MAX_CONTENT_BYTES / 1024 / 1024) + " МБ)");
        }
        try {
            Path target = Path.of(s.getFilePath());
            // Простая защита от опечатки при правке прямо в браузере — рядом остаётся ОДНА
            // предыдущая версия (перезаписывается на каждое следующее редактирование, это
            // не полноценная история версий, а "отмена последнего шага" вручную при нужде).
            if (Files.exists(target)) {
                Files.copy(target, Path.of(target + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(target, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить файл скрипта: " + e.getMessage(), e);
        }
        actionHistoryService.record(actor.uuid(), "SCRIPT_EDIT", "SCRIPT", id,
                "Отредактирован скрипт " + s.getName());
        return s;
    }
}

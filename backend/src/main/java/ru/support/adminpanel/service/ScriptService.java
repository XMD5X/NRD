package ru.support.adminpanel.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.support.adminpanel.config.AppProperties;
import ru.support.adminpanel.entity.Role;
import ru.support.adminpanel.entity.ScriptEntity;
import ru.support.adminpanel.entity.ScriptType;
import ru.support.adminpanel.repository.ScriptRepository;
import ru.support.adminpanel.security.CurrentUser;

import java.io.IOException;
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
            String safeName = UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path target = dir.resolve(safeName);
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
}

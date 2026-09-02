package ru.support.adminpanel.service;

import org.springframework.stereotype.Service;
import ru.support.adminpanel.entity.SystemSetting;
import ru.support.adminpanel.repository.SystemSettingRepository;

import java.util.Set;

/** Настройки системы, в частности переключаемый уровень логирования фронтенда (см. HLD, раздел 6). */
@Service
public class SystemSettingService {

    public static final String FRONTEND_LOG_LEVEL_KEY = "frontend_log_level";
    private static final Set<String> VALID_LEVELS = Set.of("INFO", "DEBUG", "WARNING", "TRACE");

    private final SystemSettingRepository repository;

    public SystemSettingService(SystemSettingRepository repository) {
        this.repository = repository;
    }

    public String getFrontendLogLevel() {
        return repository.findById(FRONTEND_LOG_LEVEL_KEY)
                .map(SystemSetting::getValue)
                .orElse("INFO");
    }

    public void setFrontendLogLevel(String level) {
        String normalized = level == null ? "INFO" : level.toUpperCase();
        if (!VALID_LEVELS.contains(normalized)) {
            throw new IllegalArgumentException("Недопустимый уровень логирования: " + level);
        }
        repository.save(new SystemSetting(FRONTEND_LOG_LEVEL_KEY, normalized));
    }
}

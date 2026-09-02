package ru.support.adminpanel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Настройки приложения из application.yml (секция app.*).
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Каталог для хранения загруженных файлов скриптов. */
    private String scriptsDir = "./data/scripts";

    /** Каталог для файлов результатов выполнения скриптов. */
    private String resultsDir = "./data/results";

    /** Каталог для загруженных бизнес-пользователем файлов (сертификаты, счета). */
    private String uploadsDir = "./data/uploads";

    /** Каталог для файловых логов фронтенда. */
    private String frontendLogsDir = "./data/logs/frontend";

    /** Каталог для архива файловых логов фронтенда. */
    private String frontendLogsArchiveDir = "./data/logs/frontend/archive";

    /** Секрет для подписи JWT. */
    private String jwtSecret = "change-me-in-production-please-use-env-var-adminpanel-secret-key";

    /** Время жизни JWT в минутах. */
    private long jwtExpirationMinutes = 480;

    /** Сколько дней хранить активные файловые логи фронтенда перед архивацией. */
    private int frontendLogRetentionDays = 7;

    public String getScriptsDir() { return scriptsDir; }
    public void setScriptsDir(String scriptsDir) { this.scriptsDir = scriptsDir; }

    public String getResultsDir() { return resultsDir; }
    public void setResultsDir(String resultsDir) { this.resultsDir = resultsDir; }

    public String getUploadsDir() { return uploadsDir; }
    public void setUploadsDir(String uploadsDir) { this.uploadsDir = uploadsDir; }

    public String getFrontendLogsDir() { return frontendLogsDir; }
    public void setFrontendLogsDir(String frontendLogsDir) { this.frontendLogsDir = frontendLogsDir; }

    public String getFrontendLogsArchiveDir() { return frontendLogsArchiveDir; }
    public void setFrontendLogsArchiveDir(String frontendLogsArchiveDir) { this.frontendLogsArchiveDir = frontendLogsArchiveDir; }

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }

    public long getJwtExpirationMinutes() { return jwtExpirationMinutes; }
    public void setJwtExpirationMinutes(long jwtExpirationMinutes) { this.jwtExpirationMinutes = jwtExpirationMinutes; }

    public int getFrontendLogRetentionDays() { return frontendLogRetentionDays; }
    public void setFrontendLogRetentionDays(int frontendLogRetentionDays) { this.frontendLogRetentionDays = frontendLogRetentionDays; }
}

package ru.support.adminpanel.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Метрики backend-процесса для карточки "Системные ресурсы" на странице
 * /admin/settings. Намеренно НЕ включает метрики контейнера nginx (frontend) —
 * получить их без доступа к Docker API (docker.sock) невозможно, а пробрасывать
 * его в backend означало бы дать backend'у фактически root-доступ к хосту
 * (см. аудит безопасности) ради строчки "CPU фронтенда" — компромисс того не
 * стоит. Место, занимаемое собранной статикой фронтенда, показывается отдельно,
 * напрямую с nginx (см. nginx/generate-metrics.sh, GET /nginx-metrics.json).
 */
public class SystemMetricsResponse {

    /** Загрузка CPU самим backend-процессом, 0..100. Null, если JVM не смогла её измерить. */
    private Double processCpuLoadPercent;

    /** Загрузка CPU всего хоста/контейнера, 0..100. Null, если недоступна. */
    private Double systemCpuLoadPercent;

    private int availableProcessors;

    private long heapUsedBytes;
    private long heapMaxBytes;

    /** Физическая память (с учётом cgroup-лимита контейнера на Java 21). Null, если недоступна. */
    private Long memoryUsedBytes;
    private Long memoryTotalBytes;

    /** Диск тома, где лежат данные приложения (/app/data). Null, если недоступен. */
    private Long diskUsableBytes;
    private Long diskTotalBytes;

    /** Размер БД PostgreSQL (pg_database_size). Null, если запрос не удался. */
    private Long databaseBytes;

    private List<DirUsage> dataDirs;

    private OffsetDateTime generatedAt = OffsetDateTime.now();

    public Double getProcessCpuLoadPercent() { return processCpuLoadPercent; }
    public void setProcessCpuLoadPercent(Double v) { this.processCpuLoadPercent = v; }

    public Double getSystemCpuLoadPercent() { return systemCpuLoadPercent; }
    public void setSystemCpuLoadPercent(Double v) { this.systemCpuLoadPercent = v; }

    public int getAvailableProcessors() { return availableProcessors; }
    public void setAvailableProcessors(int v) { this.availableProcessors = v; }

    public long getHeapUsedBytes() { return heapUsedBytes; }
    public void setHeapUsedBytes(long v) { this.heapUsedBytes = v; }

    public long getHeapMaxBytes() { return heapMaxBytes; }
    public void setHeapMaxBytes(long v) { this.heapMaxBytes = v; }

    public Long getMemoryUsedBytes() { return memoryUsedBytes; }
    public void setMemoryUsedBytes(Long v) { this.memoryUsedBytes = v; }

    public Long getMemoryTotalBytes() { return memoryTotalBytes; }
    public void setMemoryTotalBytes(Long v) { this.memoryTotalBytes = v; }

    public Long getDiskUsableBytes() { return diskUsableBytes; }
    public void setDiskUsableBytes(Long v) { this.diskUsableBytes = v; }

    public Long getDiskTotalBytes() { return diskTotalBytes; }
    public void setDiskTotalBytes(Long v) { this.diskTotalBytes = v; }

    public Long getDatabaseBytes() { return databaseBytes; }
    public void setDatabaseBytes(Long v) { this.databaseBytes = v; }

    public List<DirUsage> getDataDirs() { return dataDirs; }
    public void setDataDirs(List<DirUsage> v) { this.dataDirs = v; }

    public OffsetDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(OffsetDateTime v) { this.generatedAt = v; }

    public static class DirUsage {
        private String label;
        private String path;
        private long bytes;

        public DirUsage() {
        }

        public DirUsage(String label, String path, long bytes) {
            this.label = label;
            this.path = path;
            this.bytes = bytes;
        }

        public String getLabel() { return label; }
        public void setLabel(String v) { this.label = v; }

        public String getPath() { return path; }
        public void setPath(String v) { this.path = v; }

        public long getBytes() { return bytes; }
        public void setBytes(long v) { this.bytes = v; }
    }
}

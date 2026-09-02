package ru.support.adminpanel.service;

import com.sun.management.OperatingSystemMXBean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.support.adminpanel.config.AppProperties;
import ru.support.adminpanel.dto.SystemMetricsResponse;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Метрики backend-процесса и места на диске для карточки "Системные ресурсы"
 * (/admin/settings). Собираются "по требованию" (по GET-запросу с фронтенда),
 * без фонового опроса — это сведения о моменте запроса, а не непрерывный мониторинг.
 */
@Service
public class SystemMetricsService {

    private static final Logger log = LoggerFactory.getLogger(SystemMetricsService.class);

    private final AppProperties props;

    @PersistenceContext
    private EntityManager entityManager;

    public SystemMetricsService(AppProperties props) {
        this.props = props;
    }

    public SystemMetricsResponse collect() {
        SystemMetricsResponse response = new SystemMetricsResponse();

        // com.sun.management.OperatingSystemMXBean — расширение стандартного JMX-бина
        // с CPU/RAM, специфичное для HotSpot (есть в eclipse-temurin). На случай
        // другой JVM без этого расширения — не валим весь эндпоинт, а просто не
        // показываем эти конкретные цифры (диск/каталоги/БД посчитаются всё равно).
        try {
            var osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            response.setAvailableProcessors(osBean.getAvailableProcessors());
            response.setProcessCpuLoadPercent(toPercent(osBean.getProcessCpuLoad()));
            response.setSystemCpuLoadPercent(toPercent(osBean.getCpuLoad()));
            // getTotalMemorySize()/getFreeMemorySize() (JDK 14+) — в отличие от
            // устаревших getTotal/FreePhysicalMemorySize, внутри контейнера с
            // указанным лимитом памяти (docker-compose/Windows-сервер) отражают
            // именно лимит контейнера, а не память всего хоста.
            long totalMemory = osBean.getTotalMemorySize();
            long freeMemory = osBean.getFreeMemorySize();
            if (totalMemory > 0) {
                response.setMemoryTotalBytes(totalMemory);
                response.setMemoryUsedBytes(totalMemory - freeMemory);
            }
        } catch (ClassCastException e) {
            response.setAvailableProcessors(Runtime.getRuntime().availableProcessors());
            log.warn("com.sun.management.OperatingSystemMXBean недоступен в этой JVM: {}", e.getMessage());
        }

        Runtime runtime = Runtime.getRuntime();
        response.setHeapMaxBytes(runtime.maxMemory());
        response.setHeapUsedBytes(runtime.totalMemory() - runtime.freeMemory());

        fillDiskUsage(response);
        fillDataDirs(response);
        response.setDatabaseBytes(fetchDatabaseSize());

        return response;
    }

    private Double toPercent(double load) {
        // getProcessCpuLoad()/getCpuLoad() возвращают -1, если значение ещё
        // не успело накопиться (первые доли секунды после старта JVM) или
        // недоступно на данной платформе.
        if (load < 0) {
            return null;
        }
        return Math.round(load * 1000.0) / 10.0;
    }

    private void fillDiskUsage(SystemMetricsResponse response) {
        try {
            Path anchor = resolveExistingAncestor(Path.of(props.getScriptsDir()));
            if (anchor == null) {
                return;
            }
            FileStore store = Files.getFileStore(anchor);
            response.setDiskTotalBytes(store.getTotalSpace());
            response.setDiskUsableBytes(store.getUsableSpace());
        } catch (IOException e) {
            log.warn("Не удалось получить размер диска для метрик: {}", e.getMessage());
        }
    }

    private void fillDataDirs(SystemMetricsResponse response) {
        List<SystemMetricsResponse.DirUsage> dirs = new ArrayList<>();
        dirs.add(new SystemMetricsResponse.DirUsage("Скрипты", props.getScriptsDir(), directorySize(props.getScriptsDir())));
        dirs.add(new SystemMetricsResponse.DirUsage("Результаты выполнения", props.getResultsDir(), directorySize(props.getResultsDir())));
        dirs.add(new SystemMetricsResponse.DirUsage("Загрузки бизнес-пользователей", props.getUploadsDir(), directorySize(props.getUploadsDir())));
        long frontendLogs = directorySize(props.getFrontendLogsDir()) + directorySize(props.getFrontendLogsArchiveDir());
        dirs.add(new SystemMetricsResponse.DirUsage("Логи фронтенда", props.getFrontendLogsDir(), frontendLogs));
        dirs.add(new SystemMetricsResponse.DirUsage("Логи backend", props.getBackendLogsDir(), directorySize(props.getBackendLogsDir())));
        response.setDataDirs(dirs);
    }

    private long directorySize(String dir) {
        Path path = Path.of(dir);
        if (!Files.isDirectory(path)) {
            return 0L;
        }
        try (var walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            log.warn("Не удалось посчитать размер каталога {}: {}", dir, e.getMessage());
            return 0L;
        }
    }

    /** Поднимается вверх по дереву каталогов до первого реально существующего —
     *  на "чистом" стенде каталоги данных ещё не созданы (ни одного скрипта не
     *  заливали), поэтому берём в качестве точки отсчёта ближайшего существующего родителя. */
    private Path resolveExistingAncestor(Path path) {
        Path candidate = path.toAbsolutePath().normalize();
        while (candidate != null && !Files.exists(candidate)) {
            candidate = candidate.getParent();
        }
        return candidate;
    }

    private Long fetchDatabaseSize() {
        try {
            Object result = entityManager.createNativeQuery("SELECT pg_database_size(current_database())").getSingleResult();
            if (result instanceof Number number) {
                return number.longValue();
            }
            return null;
        } catch (Exception e) {
            log.warn("Не удалось получить размер БД: {}", e.getMessage());
            return null;
        }
    }
}

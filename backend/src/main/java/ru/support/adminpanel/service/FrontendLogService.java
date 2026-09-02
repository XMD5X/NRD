package ru.support.adminpanel.service;

import org.springframework.stereotype.Service;
import ru.support.adminpanel.config.AppProperties;
import ru.support.adminpanel.dto.FrontendLogEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.FileOutputStream;
import java.io.File;

/**
 * Приём и запись файловых логов фронтенда (INFO/DEBUG/WARNING/TRACE, см. HLD раздел 6).
 * Ротация: 7 дней хранения активных файлов, затем архивация в zip.
 */
@Service
public class FrontendLogService {

    private final AppProperties props;
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public FrontendLogService(AppProperties props) {
        this.props = props;
    }

    public synchronized void appendBatch(List<FrontendLogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        try {
            Path dir = Path.of(props.getFrontendLogsDir());
            Files.createDirectories(dir);
            String fileName = "frontend-" + LocalDate.now().format(FILE_DATE) + ".log";
            Path file = dir.resolve(fileName);
            StringBuilder sb = new StringBuilder();
            for (FrontendLogEntry e : entries) {
                sb.append(String.format("[%s] %-7s %s %s%n",
                        e.getTimestamp(), e.getLevel(), e.getContext() == null ? "" : e.getContext(), e.getMessage()));
            }
            Files.writeString(file, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось записать лог фронтенда: " + e.getMessage(), e);
        }
    }

    /**
     * Архивация файлов логов старше N дней (по умолчанию 7, app.frontendLogRetentionDays).
     * Запускается по расписанию (см. LogRotationScheduler) и может быть вызвана вручную.
     */
    public void rotateAndArchive() {
        try {
            Path dir = Path.of(props.getFrontendLogsDir());
            if (!Files.exists(dir)) {
                return;
            }
            Path archiveDir = Path.of(props.getFrontendLogsArchiveDir());
            Files.createDirectories(archiveDir);

            long cutoffMillis = System.currentTimeMillis() - (props.getFrontendLogRetentionDays() * 24L * 60 * 60 * 1000);

            try (var stream = Files.list(dir)) {
                for (Path p : stream.toList()) {
                    if (Files.isRegularFile(p) && p.toString().endsWith(".log")) {
                        if (p.toFile().lastModified() < cutoffMillis) {
                            archiveFile(p, archiveDir);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка ротации логов: " + e.getMessage(), e);
        }
    }

    private void archiveFile(Path file, Path archiveDir) throws IOException {
        Path zipPath = archiveDir.resolve(file.getFileName().toString() + ".zip");
        try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zos.putNextEntry(new ZipEntry(file.getFileName().toString()));
            Files.copy(file, zos);
            zos.closeEntry();
        }
        Files.delete(file);
    }
}

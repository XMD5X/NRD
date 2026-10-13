package ru.support.adminpanel.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.support.adminpanel.config.AppProperties;
import ru.support.adminpanel.dto.LogLevelRequest;
import ru.support.adminpanel.dto.SystemMetricsResponse;
import ru.support.adminpanel.service.CefAuditExportService;
import ru.support.adminpanel.service.SystemMetricsService;
import ru.support.adminpanel.service.SystemSettingService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SystemSettingService settingService;
    private final SystemMetricsService systemMetricsService;
    private final CefAuditExportService cefAuditExportService;
    private final AppProperties appProperties;

    public SystemController(SystemSettingService settingService,
                             SystemMetricsService systemMetricsService,
                             CefAuditExportService cefAuditExportService,
                             AppProperties appProperties) {
        this.settingService = settingService;
        this.systemMetricsService = systemMetricsService;
        this.cefAuditExportService = cefAuditExportService;
        this.appProperties = appProperties;
    }

    @GetMapping("/log-level")
    public Map<String, String> getLogLevel() {
        return Map.of("level", settingService.getFrontendLogLevel());
    }

    @PutMapping("/log-level")
    public Map<String, String> setLogLevel(@RequestBody LogLevelRequest request) {
        settingService.setFrontendLogLevel(request.getLevel());
        return Map.of("level", settingService.getFrontendLogLevel());
    }

    /** Только ADMIN (см. SecurityConfig) — карточка "Системные ресурсы" на /admin/settings. */
    @GetMapping("/metrics")
    public SystemMetricsResponse metrics() {
        return systemMetricsService.collect();
    }

    /** Архив (zip) всех файлов логов backend — если их несколько (ротация по дням/размеру). */
    @GetMapping("/logs/backend")
    public ResponseEntity<ByteArrayResource> downloadBackendLogs() throws IOException {
        return downloadDirectoryAsZip(Path.of(appProperties.getBackendLogsDir()), "backend");
    }

    /** Архив (zip) активных и уже заархивированных файлов логов фронтенда. */
    @GetMapping("/logs/frontend")
    public ResponseEntity<ByteArrayResource> downloadFrontendLogs() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(buffer)) {
            addDirectoryToZip(zos, Path.of(appProperties.getFrontendLogsDir()), "active");
            addDirectoryToZip(zos, Path.of(appProperties.getFrontendLogsArchiveDir()), "archive");
        }
        return zipResponse(buffer.toByteArray(), "frontend");
    }

    /** Журнал безопасности (входы + действия пользователей) в формате CEF, одним файлом. */
    @GetMapping("/logs/security-cef")
    public ResponseEntity<ByteArrayResource> downloadSecurityCef() {
        byte[] content = cefAuditExportService.buildCefLog().getBytes(StandardCharsets.UTF_8);
        String filename = LocalDate.now().format(FILE_DATE) + "-cef.log";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.TEXT_PLAIN)
                .body(new ByteArrayResource(content));
    }

    private ResponseEntity<ByteArrayResource> downloadDirectoryAsZip(Path dir, String namePrefix) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(buffer)) {
            addDirectoryToZip(zos, dir, null);
        }
        return zipResponse(buffer.toByteArray(), namePrefix);
    }

    private void addDirectoryToZip(ZipOutputStream zos, Path dir, String prefix) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        List<Path> files;
        try (var stream = Files.walk(dir)) {
            files = stream.filter(Files::isRegularFile).sorted(Comparator.naturalOrder()).toList();
        }
        for (Path file : files) {
            String entryName = dir.relativize(file).toString().replace('\\', '/');
            if (prefix != null) {
                entryName = prefix + "/" + entryName;
            }
            zos.putNextEntry(new ZipEntry(entryName));
            Files.copy(file, zos);
            zos.closeEntry();
        }
    }

    private ResponseEntity<ByteArrayResource> zipResponse(byte[] content, String namePrefix) {
        // Имя = дата + тип лога (см. пожелание "дата + наименование, бэк или фронт") —
        // так файлы за разные дни сортируются по имени в порядке дат, а не вперемешку
        // по префиксу "backend"/"frontend".
        String filename = LocalDate.now().format(FILE_DATE) + "-" + namePrefix + ".zip";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(content));
    }
}

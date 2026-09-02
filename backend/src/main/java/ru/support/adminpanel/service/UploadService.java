package ru.support.adminpanel.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.support.adminpanel.config.AppProperties;
import ru.support.adminpanel.entity.UploadedFile;
import ru.support.adminpanel.entity.UploadedFileType;
import ru.support.adminpanel.repository.UploadedFileRepository;
import ru.support.adminpanel.security.CurrentUser;
import ru.support.adminpanel.util.SafeFileNames;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Загрузка сертификатов/счетов/полномочий бизнес-пользователем (US-4, REFINED_VISION.md).
 * Файлы хранятся на диске, метаданные — в БД (решение из SA-интервью).
 */
@Service
public class UploadService {

    private final UploadedFileRepository repository;
    private final AppProperties props;
    private final ActionHistoryService actionHistoryService;

    public UploadService(UploadedFileRepository repository, AppProperties props,
                          ActionHistoryService actionHistoryService) {
        this.repository = repository;
        this.props = props;
        this.actionHistoryService = actionHistoryService;
    }

    public UploadedFile upload(MultipartFile file, UploadedFileType type, CurrentUser actor) {
        try {
            Path dir = Path.of(props.getUploadsDir());
            Files.createDirectories(dir);
            // См. SafeFileNames: имя файла от клиента нельзя использовать как есть — путь
            // traversal через "../" в оригинальном имени файла (аудит безопасности).
            String storedName = UUID.randomUUID() + "-" + SafeFileNames.sanitize(file.getOriginalFilename());
            Path target = SafeFileNames.resolveInside(dir, storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            UploadedFile uf = new UploadedFile();
            uf.setUserId(actor.uuid());
            uf.setFileType(type);
            uf.setOriginalName(file.getOriginalFilename());
            uf.setStoredPath(target.toString());
            UploadedFile saved = repository.save(uf);

            actionHistoryService.record(actor.uuid(), "FILE_UPLOAD", "UPLOADED_FILE", saved.getId(),
                    "Тип: " + type + ", файл: " + file.getOriginalFilename());
            return saved;
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить файл: " + e.getMessage(), e);
        }
    }

    public List<UploadedFile> myUploads(UUID userId) {
        return repository.findByUserIdOrderByUploadedAtDesc(userId);
    }
}

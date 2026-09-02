package ru.support.adminpanel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.support.adminpanel.entity.UploadedFile;

import java.util.List;
import java.util.UUID;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {
    List<UploadedFile> findByUserIdOrderByUploadedAtDesc(UUID userId);
}

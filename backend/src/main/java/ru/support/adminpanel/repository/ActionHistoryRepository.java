package ru.support.adminpanel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.support.adminpanel.entity.ActionHistory;

import java.util.List;
import java.util.UUID;

public interface ActionHistoryRepository extends JpaRepository<ActionHistory, UUID> {
    List<ActionHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<ActionHistory> findAllByOrderByCreatedAtDesc();

    /** Для выгрузки журнала безопасности в формате CEF (см. CefAuditExportService). */
    List<ActionHistory> findAllByOrderByCreatedAtAsc();
}

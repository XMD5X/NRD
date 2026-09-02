package ru.support.adminpanel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.support.adminpanel.entity.LoginHistory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {
    List<LoginHistory> findByUserIdOrderByAttemptedAtDesc(UUID userId);

    /** Для защиты от подбора пароля (см. AuthService) — считаем по строке логина,
     *  а не по userId, чтобы блокировать перебор и для несуществующих логинов тоже. */
    long countByLoginAttemptedIgnoreCaseAndSuccessFalseAndAttemptedAtAfter(String login, OffsetDateTime since);
}

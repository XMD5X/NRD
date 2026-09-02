package ru.support.adminpanel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.support.adminpanel.entity.ScriptExecution;

import java.util.List;
import java.util.UUID;

public interface ScriptExecutionRepository extends JpaRepository<ScriptExecution, UUID> {
    List<ScriptExecution> findByUserIdOrderByStartedAtDesc(UUID userId);
    List<ScriptExecution> findAllByOrderByStartedAtDesc();
}

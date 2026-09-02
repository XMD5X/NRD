package ru.support.adminpanel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.support.adminpanel.entity.ScriptEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScriptRepository extends JpaRepository<ScriptEntity, UUID> {
    List<ScriptEntity> findByActiveTrue();
    boolean existsByName(String name);
    Optional<ScriptEntity> findByName(String name);
    List<ScriptEntity> findByCategoryAndActiveTrue(String category);
}

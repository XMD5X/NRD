package ru.support.adminpanel.service;

import org.springframework.stereotype.Service;
import ru.support.adminpanel.entity.ActionHistory;
import ru.support.adminpanel.repository.ActionHistoryRepository;

import java.util.UUID;

@Service
public class ActionHistoryService {

    private final ActionHistoryRepository repository;

    public ActionHistoryService(ActionHistoryRepository repository) {
        this.repository = repository;
    }

    public void record(UUID userId, String actionType, String entityType, UUID entityId, String details) {
        ActionHistory h = new ActionHistory();
        h.setUserId(userId);
        h.setActionType(actionType);
        h.setEntityType(entityType);
        h.setEntityId(entityId);
        h.setDetails(details);
        repository.save(h);
    }
}

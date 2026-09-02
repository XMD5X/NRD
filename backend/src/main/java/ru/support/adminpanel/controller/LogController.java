package ru.support.adminpanel.controller;

import org.springframework.web.bind.annotation.*;
import ru.support.adminpanel.dto.FrontendLogEntry;
import ru.support.adminpanel.service.FrontendLogService;

import java.util.List;

/** Приём батчей логов с фронтенда (INFO/DEBUG/WARNING/TRACE), см. HLD раздел 6. */
@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final FrontendLogService frontendLogService;

    public LogController(FrontendLogService frontendLogService) {
        this.frontendLogService = frontendLogService;
    }

    @PostMapping
    public void ingest(@RequestBody List<FrontendLogEntry> entries) {
        frontendLogService.appendBatch(entries);
    }
}

package ru.support.adminpanel.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Ежедневная ротация/архивация файловых логов фронтенда (см. HLD раздел 6). */
@Component
public class LogRotationScheduler {

    private final FrontendLogService frontendLogService;

    public LogRotationScheduler(FrontendLogService frontendLogService) {
        this.frontendLogService = frontendLogService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void rotate() {
        frontendLogService.rotateAndArchive();
    }
}

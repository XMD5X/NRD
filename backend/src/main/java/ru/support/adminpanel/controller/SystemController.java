package ru.support.adminpanel.controller;

import org.springframework.web.bind.annotation.*;
import ru.support.adminpanel.dto.LogLevelRequest;
import ru.support.adminpanel.service.SystemSettingService;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemSettingService settingService;

    public SystemController(SystemSettingService settingService) {
        this.settingService = settingService;
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
}

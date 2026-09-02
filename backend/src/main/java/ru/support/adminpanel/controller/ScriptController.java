package ru.support.adminpanel.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.support.adminpanel.dto.ExecuteScriptRequest;
import ru.support.adminpanel.dto.ExecutionResponse;
import ru.support.adminpanel.dto.ScriptResponse;
import ru.support.adminpanel.entity.Role;
import ru.support.adminpanel.entity.ScriptType;
import ru.support.adminpanel.security.CurrentUserUtil;
import ru.support.adminpanel.service.ExecutionService;
import ru.support.adminpanel.service.ScriptService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scripts")
public class ScriptController {

    private final ScriptService scriptService;
    private final ExecutionService executionService;

    public ScriptController(ScriptService scriptService, ExecutionService executionService) {
        this.scriptService = scriptService;
        this.executionService = executionService;
    }

    @GetMapping
    public List<ScriptResponse> list() {
        var role = CurrentUserUtil.get().role();
        return scriptService.listForRole(role).stream().map(ScriptResponse::from).toList();
    }

    @PostMapping
    public ScriptResponse upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam String name,
                                  @RequestParam(required = false) String description,
                                  @RequestParam ScriptType scriptType,
                                  @RequestParam(required = false, defaultValue = "[]") String parametersConfig,
                                  @RequestParam(required = false) Role visibleToRole,
                                  @RequestParam(required = false) String category,
                                  @RequestParam(required = false) String bankName) {
        var saved = scriptService.upload(name, description, scriptType, parametersConfig,
                visibleToRole, category, bankName, file, CurrentUserUtil.get());
        return ScriptResponse.from(saved);
    }

    @PatchMapping("/{id}/toggle")
    public ScriptResponse toggle(@PathVariable UUID id) {
        return ScriptResponse.from(scriptService.toggleActive(id, CurrentUserUtil.get()));
    }

    @PostMapping("/{id}/execute")
    public ExecutionResponse execute(@PathVariable UUID id, @RequestBody(required = false) ExecuteScriptRequest request) {
        var params = request == null ? null : request.getParameters();
        var execution = executionService.execute(id, params, CurrentUserUtil.get());
        return ExecutionResponse.from(execution, executionService.listResultFiles(execution).size());
    }
}

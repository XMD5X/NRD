package ru.support.adminpanel.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.support.adminpanel.dto.ExecuteScriptRequest;
import ru.support.adminpanel.dto.ExecutionResponse;
import ru.support.adminpanel.dto.ScriptResponse;
import ru.support.adminpanel.entity.Role;
import ru.support.adminpanel.entity.ScriptType;
import ru.support.adminpanel.security.CurrentUserUtil;
import ru.support.adminpanel.service.BatchExcelService;
import ru.support.adminpanel.service.ExecutionService;
import ru.support.adminpanel.service.ScriptService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scripts")
public class ScriptController {

    private final ScriptService scriptService;
    private final ExecutionService executionService;
    private final BatchExcelService batchExcelService;

    public ScriptController(ScriptService scriptService, ExecutionService executionService,
                             BatchExcelService batchExcelService) {
        this.scriptService = scriptService;
        this.executionService = executionService;
        this.batchExcelService = batchExcelService;
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

    /**
     * Массовый запуск задачи "Выдача прав доступа..." сразу по всем банкам эталонной
     * роли — вместо одного банка и списка счетов пользователь прикладывает Excel-файл
     * "Счёт / Банк" (см. кнопку "Все банки" в TaskDetailPage.jsx на фронте).
     */
    @PostMapping("/execute-batch")
    public ExecutionResponse executeBatch(@RequestParam String category,
                                           @RequestParam(required = false) String userId,
                                           @RequestParam("file") MultipartFile file) {
        var result = executionService.executeBatch(category, userId, file, CurrentUserUtil.get());
        return ExecutionResponse.from(result.execution(),
                executionService.listResultFiles(result.execution()).size(), result.banks());
    }

    /** Шаблон .xlsx (Счёт/Банк) для загрузки в режиме "Все банки" — со списком банков роли. */
    @GetMapping("/execute-batch/template")
    public ResponseEntity<ByteArrayResource> batchTemplate(@RequestParam String category) {
        List<String> banks = executionService.bankNamesForCategory(category);
        byte[] bytes = batchExcelService.buildTemplate(banks);
        String filename = URLEncoder.encode("Шаблон_счета_банки.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(bytes));
    }
}

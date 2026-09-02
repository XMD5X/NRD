package ru.support.adminpanel.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.support.adminpanel.dto.ExecutionHistoryResponse;
import ru.support.adminpanel.dto.ExecutionResponse;
import ru.support.adminpanel.entity.ScriptExecution;
import ru.support.adminpanel.security.CurrentUserUtil;
import ru.support.adminpanel.service.ExecutionService;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    /**
     * Общая история всех выполнений всех пользователей ("кто, когда и что генерировал") —
     * страница "История" во фронтенде. Просмотр и скачивание доступны любому
     * авторизованному пользователю (см. HLD: общий инструмент поддержки), а не только
     * автору или администратору — иначе сотрудники не смогут скачать файлы друг друга.
     */
    @GetMapping
    public List<ExecutionHistoryResponse> all() {
        return executionService.allExecutionsWithDetails();
    }

    @GetMapping("/mine")
    public List<ExecutionResponse> mine() {
        return executionService.myExecutions(CurrentUserUtil.get().uuid())
                .stream()
                .map(e -> ExecutionResponse.from(e, executionService.listResultFiles(e).size()))
                .toList();
    }

    @GetMapping("/{id}")
    public ExecutionResponse get(@PathVariable UUID id) {
        ScriptExecution execution = executionService.getOrThrow(id);
        return ExecutionResponse.from(execution, executionService.listResultFiles(execution).size());
    }

    /**
     * Отдаёт файлы результата: если файл один — как есть, если несколько (по одному
     * на каждый введённый через запятую счёт) — упаковывает в zip-архив на лету.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) throws IOException {
        ScriptExecution execution = executionService.getOrThrow(id);
        List<File> files = executionService.listResultFiles(execution);
        if (files.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (files.size() == 1) {
            File file = files.get(0);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .body(new FileSystemResource(file));
        }
        byte[] zipBytes = executionService.zipResultFiles(files);
        String zipName = "execution_" + id + "_results.zip";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(zipBytes));
    }

    @PostMapping("/{id}/send")
    public ExecutionResponse send(@PathVariable UUID id) {
        checkSendAccess(executionService.getOrThrow(id));
        ScriptExecution sent = executionService.send(id, CurrentUserUtil.get());
        return ExecutionResponse.from(sent, executionService.listResultFiles(sent).size());
    }

    /** Отправку в целевую систему по-прежнему может инициировать только автор или администратор. */
    private ScriptExecution checkSendAccess(ScriptExecution execution) {
        var current = CurrentUserUtil.get();
        if (!"ADMIN".equals(current.role()) && !execution.getUserId().equals(current.uuid())) {
            throw new SecurityException("Нет доступа к этому выполнению");
        }
        return execution;
    }
}

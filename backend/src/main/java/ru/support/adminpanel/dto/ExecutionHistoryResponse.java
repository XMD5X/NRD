package ru.support.adminpanel.dto;

import ru.support.adminpanel.entity.ExecutionStatus;
import ru.support.adminpanel.entity.ScriptEntity;
import ru.support.adminpanel.entity.ScriptExecution;
import ru.support.adminpanel.entity.User;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Строка общей истории выполнений ("кто, когда и что генерировал") — см. запрос
 * пользователя про доступность сгенерированных файлов из истории для последующей
 * отправки на фронт. В отличие от ExecutionResponse, содержит человекочитаемые
 * поля (логин пользователя, название задачи/банка/категории), чтобы фронту не
 * нужно было отдельно подгружать /api/scripts и /api/users для отображения таблицы.
 */
public class ExecutionHistoryResponse {
    private UUID id;
    private UUID scriptId;
    private String scriptName;
    private String category;
    private String bankName;
    private UUID userId;
    private String userLogin;
    private ExecutionStatus status;
    private boolean hasResultFile;
    /** Сколько файлов результата сгенерировано (по одному на каждый введённый счёт). */
    private int resultFileCount;
    /** Заполняется только когда файл ровно один — иначе скачивание отдаёт zip-архив. */
    private String resultFileName;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private OffsetDateTime sentToTargetAt;

    public static ExecutionHistoryResponse from(ScriptExecution e, ScriptEntity script, User user, List<File> resultFiles) {
        ExecutionHistoryResponse r = new ExecutionHistoryResponse();
        r.id = e.getId();
        r.scriptId = e.getScriptId();
        r.scriptName = script != null ? script.getName() : null;
        r.category = script != null ? script.getCategory() : null;
        r.bankName = script != null ? script.getBankName() : null;
        r.userId = e.getUserId();
        r.userLogin = user != null ? user.getLogin() : null;
        r.status = e.getStatus();
        r.resultFileCount = resultFiles.size();
        r.hasResultFile = !resultFiles.isEmpty();
        r.resultFileName = resultFiles.size() == 1 ? resultFiles.get(0).getName() : null;
        r.startedAt = e.getStartedAt();
        r.finishedAt = e.getFinishedAt();
        r.sentToTargetAt = e.getSentToTargetAt();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getScriptId() { return scriptId; }
    public String getScriptName() { return scriptName; }
    public String getCategory() { return category; }
    public String getBankName() { return bankName; }
    public UUID getUserId() { return userId; }
    public String getUserLogin() { return userLogin; }
    public ExecutionStatus getStatus() { return status; }
    public boolean isHasResultFile() { return hasResultFile; }
    public int getResultFileCount() { return resultFileCount; }
    public String getResultFileName() { return resultFileName; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public OffsetDateTime getSentToTargetAt() { return sentToTargetAt; }
}

package ru.support.adminpanel.dto;

import ru.support.adminpanel.entity.ExecutionStatus;
import ru.support.adminpanel.entity.ScriptExecution;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ExecutionResponse {
    private UUID id;
    private UUID scriptId;
    private ExecutionStatus status;
    private String stdout;
    private String stderr;
    private boolean hasResultFile;
    /** Сколько файлов результата сгенерировано (по одному на каждый введённый счёт).
     *  Если больше одного — скачивание отдаёт zip-архив, см. ExecutionController. */
    private int resultFileCount;
    /** Только для массового запуска "Все банки" (см. ScriptController.executeBatch) —
     *  разбивка загруженных из Excel счетов по банкам, для наглядного подтверждения
     *  на UI, что именно было загружено и готово к отправке. Null для обычных запусков. */
    private List<BatchBankSummary> batchBanks;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private OffsetDateTime sentToTargetAt;

    public static ExecutionResponse from(ScriptExecution e, int resultFileCount) {
        return from(e, resultFileCount, null);
    }

    public static ExecutionResponse from(ScriptExecution e, int resultFileCount, List<BatchBankSummary> batchBanks) {
        ExecutionResponse r = new ExecutionResponse();
        r.id = e.getId();
        r.scriptId = e.getScriptId();
        r.status = e.getStatus();
        r.stdout = e.getStdout();
        r.stderr = e.getStderr();
        r.resultFileCount = resultFileCount;
        r.hasResultFile = resultFileCount > 0;
        r.batchBanks = batchBanks;
        r.startedAt = e.getStartedAt();
        r.finishedAt = e.getFinishedAt();
        r.sentToTargetAt = e.getSentToTargetAt();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getScriptId() { return scriptId; }
    public ExecutionStatus getStatus() { return status; }
    public String getStdout() { return stdout; }
    public String getStderr() { return stderr; }
    public boolean isHasResultFile() { return hasResultFile; }
    public int getResultFileCount() { return resultFileCount; }
    public List<BatchBankSummary> getBatchBanks() { return batchBanks; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public OffsetDateTime getSentToTargetAt() { return sentToTargetAt; }
}

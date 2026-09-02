package ru.support.adminpanel.dto;

import ru.support.adminpanel.entity.ScriptEntity;
import ru.support.adminpanel.entity.ScriptType;

import java.util.UUID;

public class ScriptResponse {
    private UUID id;
    private String name;
    private String description;
    private ScriptType scriptType;
    private String parametersConfig;
    private boolean active;
    private boolean hasSendStep;
    private String category;
    private String bankName;

    public static ScriptResponse from(ScriptEntity s) {
        ScriptResponse r = new ScriptResponse();
        r.id = s.getId();
        r.name = s.getName();
        r.description = s.getDescription();
        r.scriptType = s.getScriptType();
        r.parametersConfig = s.getParametersConfig();
        r.active = s.isActive();
        r.hasSendStep = s.getSendScriptPath() != null && !s.getSendScriptPath().isBlank();
        r.category = s.getCategory();
        r.bankName = s.getBankName();
        return r;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ScriptType getScriptType() { return scriptType; }
    public String getParametersConfig() { return parametersConfig; }
    public boolean isActive() { return active; }
    public boolean isHasSendStep() { return hasSendStep; }
    public String getCategory() { return category; }
    public String getBankName() { return bankName; }
}

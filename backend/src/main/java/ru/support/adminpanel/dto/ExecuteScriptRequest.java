package ru.support.adminpanel.dto;

import java.util.Map;

public class ExecuteScriptRequest {
    private Map<String, String> parameters;

    public Map<String, String> getParameters() { return parameters; }
    public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }
}

package ru.support.adminpanel.dto;

public class FrontendLogEntry {
    private String level;
    private String message;
    private String timestamp;
    private String context;

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
}

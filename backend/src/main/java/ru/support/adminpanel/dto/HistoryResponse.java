package ru.support.adminpanel.dto;

import ru.support.adminpanel.entity.ActionHistory;
import ru.support.adminpanel.entity.LoginHistory;

import java.util.List;

public class HistoryResponse {
    private List<LoginHistory> loginHistory;
    private List<ActionHistory> actionHistory;

    public HistoryResponse(List<LoginHistory> loginHistory, List<ActionHistory> actionHistory) {
        this.loginHistory = loginHistory;
        this.actionHistory = actionHistory;
    }

    public List<LoginHistory> getLoginHistory() { return loginHistory; }
    public List<ActionHistory> getActionHistory() { return actionHistory; }
}

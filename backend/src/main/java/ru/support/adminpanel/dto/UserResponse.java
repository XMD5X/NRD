package ru.support.adminpanel.dto;

import ru.support.adminpanel.entity.Role;
import ru.support.adminpanel.entity.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public class UserResponse {
    private UUID id;
    private String login;
    private Role role;
    private boolean blocked;
    private String blockedReason;
    private OffsetDateTime createdAt;
    private boolean online;

    public static UserResponse from(User u, boolean online) {
        UserResponse r = new UserResponse();
        r.id = u.getId();
        r.login = u.getLogin();
        r.role = u.getRole();
        r.blocked = u.isBlocked();
        r.blockedReason = u.getBlockedReason();
        r.createdAt = u.getCreatedAt();
        r.online = online;
        return r;
    }

    public UUID getId() { return id; }
    public String getLogin() { return login; }
    public Role getRole() { return role; }
    public boolean isBlocked() { return blocked; }
    public String getBlockedReason() { return blockedReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public boolean isOnline() { return online; }
}

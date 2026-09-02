package ru.support.adminpanel.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "login_history")
public class LoginHistory {

    @Id
    @GeneratedValue
    private UUID id;

    /** Может быть null, если логин не найден в системе (попытка входа под несуществующим пользователем). */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "login_attempted", nullable = false)
    private String loginAttempted;

    @Column(name = "attempted_at", nullable = false)
    private OffsetDateTime attemptedAt = OffsetDateTime.now();

    @Column(nullable = false)
    private boolean success;

    @Column(name = "ip_address")
    private String ipAddress;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getLoginAttempted() { return loginAttempted; }
    public void setLoginAttempted(String loginAttempted) { this.loginAttempted = loginAttempted; }

    public OffsetDateTime getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(OffsetDateTime attemptedAt) { this.attemptedAt = attemptedAt; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}

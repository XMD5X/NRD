package ru.support.adminpanel.dto;

import jakarta.validation.constraints.NotBlank;
import ru.support.adminpanel.entity.Role;

public class CreateUserRequest {
    @NotBlank
    private String login;
    @NotBlank
    private String password;
    private Role role = Role.BUSINESS_USER;

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}

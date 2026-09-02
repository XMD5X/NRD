package ru.support.adminpanel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.support.adminpanel.entity.Role;

public class CreateUserRequest {
    @NotBlank
    private String login;
    // Минимальная длина пароля (см. аудит безопасности — раньше это поле вообще
    // не проверялось: аннотации валидации были, но контроллер не был помечен
    // @Valid, так что они молча игнорировались).
    @NotBlank
    @Size(min = 8, message = "Пароль должен быть не короче 8 символов")
    private String password;
    private Role role = Role.BUSINESS_USER;

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}

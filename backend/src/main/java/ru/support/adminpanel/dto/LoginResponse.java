package ru.support.adminpanel.dto;

public class LoginResponse {
    private String token;
    private String login;
    private String role;

    public LoginResponse(String token, String login, String role) {
        this.token = token;
        this.login = login;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getLogin() { return login; }
    public String getRole() { return role; }
}

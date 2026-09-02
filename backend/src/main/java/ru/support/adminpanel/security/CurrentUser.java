package ru.support.adminpanel.security;

import java.util.UUID;

/** Данные текущего аутентифицированного пользователя, извлечённые из JWT. */
public record CurrentUser(String userId, String login, String role) {
    public UUID uuid() {
        return UUID.fromString(userId);
    }
}

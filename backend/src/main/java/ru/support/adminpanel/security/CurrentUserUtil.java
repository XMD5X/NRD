package ru.support.adminpanel.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUserUtil {
    private CurrentUserUtil() { }

    public static CurrentUser get() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CurrentUser cu) {
            return cu;
        }
        throw new IllegalStateException("Пользователь не аутентифицирован");
    }
}

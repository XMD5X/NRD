package ru.support.adminpanel.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.support.adminpanel.dto.LoginRequest;
import ru.support.adminpanel.dto.LoginResponse;
import ru.support.adminpanel.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest.getRemoteAddr());
    }

    @PostMapping("/logout")
    public void logout() {
        // JWT stateless — клиент просто удаляет токен на своей стороне.
    }
}

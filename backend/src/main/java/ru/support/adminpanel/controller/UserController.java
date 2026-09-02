package ru.support.adminpanel.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.support.adminpanel.dto.*;
import ru.support.adminpanel.entity.User;
import ru.support.adminpanel.security.CurrentUserUtil;
import ru.support.adminpanel.service.OnlineUsersTracker;
import ru.support.adminpanel.service.UserService;

import java.util.List;
import java.util.UUID;

/** Только для роли ADMIN — ограничение задано в SecurityConfig. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final OnlineUsersTracker onlineUsersTracker;

    public UserController(UserService userService, OnlineUsersTracker onlineUsersTracker) {
        this.userService = userService;
        this.onlineUsersTracker = onlineUsersTracker;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.list().stream()
                .map(u -> UserResponse.from(u, onlineUsersTracker.isOnline(u.getId())))
                .toList();
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        // @Valid здесь раньше отсутствовал — аннотации @NotBlank/@Size на
        // CreateUserRequest молча ничего не проверяли (см. аудит безопасности).
        User saved = userService.create(request, CurrentUserUtil.get().uuid());
        return UserResponse.from(saved, false);
    }

    @PostMapping("/{id}/block")
    public UserResponse block(@PathVariable UUID id, @RequestBody BlockUserRequest request) {
        User saved = userService.block(id, request.getReason(), CurrentUserUtil.get().uuid());
        return UserResponse.from(saved, onlineUsersTracker.isOnline(saved.getId()));
    }

    @PostMapping("/{id}/unblock")
    public UserResponse unblock(@PathVariable UUID id) {
        User saved = userService.unblock(id, CurrentUserUtil.get().uuid());
        return UserResponse.from(saved, onlineUsersTracker.isOnline(saved.getId()));
    }

    @GetMapping("/{id}/history")
    public HistoryResponse history(@PathVariable UUID id) {
        return userService.history(id);
    }
}

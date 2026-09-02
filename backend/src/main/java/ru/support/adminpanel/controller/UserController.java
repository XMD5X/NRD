package ru.support.adminpanel.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.support.adminpanel.dto.*;
import ru.support.adminpanel.security.CurrentUserUtil;
import ru.support.adminpanel.service.UserService;

import java.util.List;
import java.util.UUID;

/** Только для роли ADMIN — ограничение задано в SecurityConfig. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.list().stream().map(UserResponse::from).toList();
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        // @Valid здесь раньше отсутствовал — аннотации @NotBlank/@Size на
        // CreateUserRequest молча ничего не проверяли (см. аудит безопасности).
        return UserResponse.from(userService.create(request, CurrentUserUtil.get().uuid()));
    }

    @PostMapping("/{id}/block")
    public UserResponse block(@PathVariable UUID id, @RequestBody BlockUserRequest request) {
        return UserResponse.from(userService.block(id, request.getReason(), CurrentUserUtil.get().uuid()));
    }

    @PostMapping("/{id}/unblock")
    public UserResponse unblock(@PathVariable UUID id) {
        return UserResponse.from(userService.unblock(id, CurrentUserUtil.get().uuid()));
    }

    @GetMapping("/{id}/history")
    public HistoryResponse history(@PathVariable UUID id) {
        return userService.history(id);
    }
}

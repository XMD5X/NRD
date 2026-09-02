package ru.support.adminpanel.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.support.adminpanel.dto.CreateUserRequest;
import ru.support.adminpanel.dto.HistoryResponse;
import ru.support.adminpanel.entity.Role;
import ru.support.adminpanel.entity.User;
import ru.support.adminpanel.repository.ActionHistoryRepository;
import ru.support.adminpanel.repository.LoginHistoryRepository;
import ru.support.adminpanel.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final ActionHistoryRepository actionHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActionHistoryService actionHistoryService;

    public UserService(UserRepository userRepository,
                        LoginHistoryRepository loginHistoryRepository,
                        ActionHistoryRepository actionHistoryRepository,
                        PasswordEncoder passwordEncoder,
                        ActionHistoryService actionHistoryService) {
        this.userRepository = userRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.actionHistoryRepository = actionHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.actionHistoryService = actionHistoryService;
    }

    public List<User> list() {
        return userRepository.findAll();
    }

    public User create(CreateUserRequest req, UUID actorId) {
        if (userRepository.existsByLogin(req.getLogin())) {
            throw new IllegalArgumentException("Пользователь с таким логином уже существует");
        }
        User u = new User();
        u.setLogin(req.getLogin());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRole(req.getRole() == null ? Role.BUSINESS_USER : req.getRole());
        User saved = userRepository.save(u);
        actionHistoryService.record(actorId, "USER_CREATE", "USER", saved.getId(),
                "Создан пользователь " + saved.getLogin() + " с ролью " + saved.getRole());
        return saved;
    }

    public User block(UUID userId, String reason, UUID actorId) {
        User u = getOrThrow(userId);
        u.setBlocked(true);
        u.setBlockedReason(reason);
        u.setBlockedAt(OffsetDateTime.now());
        User saved = userRepository.save(u);
        actionHistoryService.record(actorId, "USER_BLOCK", "USER", userId, "Причина: " + reason);
        return saved;
    }

    public User unblock(UUID userId, UUID actorId) {
        User u = getOrThrow(userId);
        u.setBlocked(false);
        u.setBlockedReason(null);
        u.setBlockedAt(null);
        User saved = userRepository.save(u);
        actionHistoryService.record(actorId, "USER_UNBLOCK", "USER", userId, null);
        return saved;
    }

    public HistoryResponse history(UUID userId) {
        return new HistoryResponse(
                loginHistoryRepository.findByUserIdOrderByAttemptedAtDesc(userId),
                actionHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
        );
    }

    private User getOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }
}

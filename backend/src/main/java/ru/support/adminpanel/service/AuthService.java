package ru.support.adminpanel.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.support.adminpanel.dto.LoginRequest;
import ru.support.adminpanel.dto.LoginResponse;
import ru.support.adminpanel.entity.LoginHistory;
import ru.support.adminpanel.entity.User;
import ru.support.adminpanel.repository.LoginHistoryRepository;
import ru.support.adminpanel.repository.UserRepository;
import ru.support.adminpanel.security.JwtService;

import java.util.Optional;

/**
 * MVP-аутентификация по логину/паролю. Временное решение на период отладки —
 * см. REFINED_VISION.md и HLD.md, раздел 8 "Безопасность".
 * Дальнейший переход на AD не входит в объём данного MVP.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                        LoginHistoryRepository loginHistoryRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request, String ipAddress) {
        Optional<User> userOpt = userRepository.findByLogin(request.getLogin());

        boolean success = false;
        User user = userOpt.orElse(null);

        if (user != null && !user.isBlocked()
                && passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            success = true;
        }

        LoginHistory history = new LoginHistory();
        history.setLoginAttempted(request.getLogin());
        history.setSuccess(success);
        history.setIpAddress(ipAddress);
        if (user != null) {
            history.setUserId(user.getId());
        }
        loginHistoryRepository.save(history);

        if (!success) {
            if (user != null && user.isBlocked()) {
                throw new IllegalStateException("Пользователь заблокирован");
            }
            throw new IllegalArgumentException("Неверный логин или пароль");
        }

        String token = jwtService.generateToken(user.getLogin(), user.getRole().name(), user.getId().toString());
        return new LoginResponse(token, user.getLogin(), user.getRole().name());
    }
}

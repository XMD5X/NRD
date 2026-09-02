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

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * MVP-аутентификация по логину/паролю. Временное решение на период отладки —
 * см. REFINED_VISION.md и HLD.md, раздел 8 "Безопасность".
 * Дальнейший переход на AD не входит в объём данного MVP.
 */
@Service
public class AuthService {

    /** Защита от подбора пароля (см. аудит безопасности — раньше попытки входа
     *  ничем не были ограничены). Считаем неудачные попытки по конкретной строке
     *  логина за последние LOCKOUT_WINDOW_MINUTES — этого достаточно, чтобы
     *  сделать автоматический перебор бессмысленным, но не мешает человеку,
     *  который пару раз опечатался в пароле. */
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_WINDOW_MINUTES = 15;

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
        OffsetDateTime windowStart = OffsetDateTime.now().minusMinutes(LOCKOUT_WINDOW_MINUTES);
        long recentFailures = loginHistoryRepository
                .countByLoginAttemptedIgnoreCaseAndSuccessFalseAndAttemptedAtAfter(request.getLogin(), windowStart);
        if (recentFailures >= MAX_FAILED_ATTEMPTS) {
            // Намеренно НЕ пишем эту попытку в LoginHistory — иначе окно блокировки
            // само себя продлевало бы бесконечно при продолжающемся переборе.
            throw new IllegalStateException("Слишком много неудачных попыток входа. "
                    + "Попробуйте снова через " + LOCKOUT_WINDOW_MINUTES + " минут.");
        }

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
            // Намеренно ОДНО и то же сообщение для "нет такого логина", "неверный
            // пароль" и "пользователь заблокирован" — иначе по ответу можно было бы
            // перебором узнавать, какие логины вообще существуют в системе (user
            // enumeration) и кто из них заблокирован. Администратор видит реальную
            // причину в /api/users (там уже требуется роль ADMIN).
            throw new IllegalArgumentException("Неверный логин или пароль");
        }

        String token = jwtService.generateToken(user.getLogin(), user.getRole().name(), user.getId().toString());
        return new LoginResponse(token, user.getLogin(), user.getRole().name());
    }
}

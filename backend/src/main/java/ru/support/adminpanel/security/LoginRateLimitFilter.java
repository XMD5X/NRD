package ru.support.adminpanel.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.support.adminpanel.config.AppProperties;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ограничение частоты попыток входа — грубая защита от автоматического перебора
 * пароля. Раньше это делал nginx (limit_req на /api/auth/login); после отказа от
 * nginx защита переехала в приложение.
 *
 * Заодно исправлена давняя неприятность прежней схемы: nginx отвечал на
 * превышение лимита своей HTML-страницей 503, фронтенд не мог разобрать её и
 * показывал общее «Ошибка входа» — неотличимо от неверного пароля. Здесь ответ
 * тот же, что у остальных ошибок API ({"error": "..."}), поэтому пользователь
 * видит, что именно произошло и сколько ждать.
 *
 * Счётчик держится в памяти процесса: стенд однопроцессный, сложная схема с
 * общим хранилищем здесь была бы неоправданной. При нескольких репликах лимит
 * станет «на реплику» — для защиты от перебора этого всё ещё достаточно, но
 * если реплик будет много, счётчик стоит вынести во внешнее хранилище.
 *
 * Точная защита по конкретной учётной записи (с понятным сообщением и
 * блокировкой) — задача backend-логики входа, см. AuthService; здесь именно
 * грубый сетевой лимит по адресу источника.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final long WINDOW_MILLIS = 60_000L;
    /** Предохранитель от разрастания карты, если адресов окажется очень много. */
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final AppProperties props;
    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(AppProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        int limit = props.getLoginAttemptsPerMinute();
        if (limit <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        if (exceedsLimit(clientIp(request), limit)) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"error\":\"Слишком много попыток входа. Подождите минуту и попробуйте снова.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return LOGIN_PATH.equals(pathWithinApplication(request));
    }

    private boolean exceedsLimit(String client, int limit) {
        long now = System.currentTimeMillis();
        if (attempts.size() > MAX_TRACKED_CLIENTS) {
            attempts.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    Long last = e.getValue().peekLast();
                    return last == null || now - last > WINDOW_MILLIS;
                }
            });
        }
        Deque<Long> window = attempts.computeIfAbsent(client, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() > WINDOW_MILLIS) {
                window.pollFirst();
            }
            if (window.size() >= limit) {
                return true;
            }
            window.addLast(now);
            return false;
        }
    }

    /**
     * Адрес источника. За обратным прокси (Ingress в Kubernetes) реальный адрес
     * приходит в X-Forwarded-For — берём первый элемент цепочки. Заголовок легко
     * подделать, но для грубого лимита это приемлемо: без него за прокси все
     * клиенты выглядели бы одним адресом и лимит задевал бы всех сразу.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }

    private String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return (context != null && !context.isEmpty() && uri.startsWith(context))
                ? uri.substring(context.length())
                : uri;
    }
}

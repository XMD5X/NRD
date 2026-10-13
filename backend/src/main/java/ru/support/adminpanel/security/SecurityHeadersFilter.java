package ru.support.adminpanel.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security-заголовки ответов — раньше их проставлял nginx (см. историю:
 * nginx/common-locations.conf), а после отказа от nginx отдавать статику и API
 * стало некому, кроме самого приложения.
 *
 * Содержимое намеренно совпадает с тем, что стояло в nginx, чтобы поведение
 * стенда не изменилось: запрет фрейминга, запрет угадывания типа содержимого,
 * ограничение Referer и отключение ненужных браузерных возможностей, плюс CSP.
 *
 * В CSP script-src намеренно без 'unsafe-inline' и 'unsafe-eval' — именно это
 * реально защищает от XSS (внедрённый в HTML скрипт просто не выполнится).
 * style-src с 'unsafe-inline' оставлен сознательно: в компонентах фронтенда есть
 * инлайновые style={{...}}, без послабления интерфейс поедет.
 *
 * Фильтр стоит раньше цепочки Spring Security: её собственные писатели
 * заголовков (X-Frame-Options, X-Content-Type-Options) проверяют, не выставлен
 * ли заголовок ранее, и не дублируют его.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String CSP = "default-src 'self'; "
            + "script-src 'self'; "
            + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
            + "font-src 'self' https://fonts.gstatic.com; "
            + "img-src 'self' data:; "
            + "connect-src 'self'; "
            + "frame-ancestors 'none'; "
            + "base-uri 'self'; "
            + "form-action 'self'";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "geolocation=(), camera=(), microphone=()");
        response.setHeader("Content-Security-Policy", CSP);
        filterChain.doFilter(request, response);
    }
}

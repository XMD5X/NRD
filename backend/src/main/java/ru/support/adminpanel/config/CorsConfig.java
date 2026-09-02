package ru.support.adminpanel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS нужен только для локальной разработки, когда frontend (Vite dev server)
 * и backend работают на разных портах. В продакшене nginx проксирует /api на backend
 * с того же origin, поэтому CORS фактически не используется.
 *
 * ВАЖНО (см. аудит безопасности): раньше здесь стоял allowedOriginPatterns("*") вместе
 * с allowCredentials(true) БЕЗ привязки к профилю/окружению — то есть в проде запросы
 * с произвольного стороннего сайта тоже разрешались браузером как "credentialed".
 * Список origin теперь явный и настраиваемый через APP_CORS_ALLOWED_ORIGINS (через
 * запятую) — по умолчанию только адреса локальной разработки (Vite dev server), а не "*".
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors-allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

package ru.support.adminpanel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS-конфигурация.
 *
 * Приложение аутентифицирует запросы Bearer-токеном в заголовке Authorization
 * (см. JwtAuthFilter) — cookies для авторизации не используются нигде. Поэтому
 * credentialed-запросы (withCredentials/cookies) браузеру не нужны, и мы можем
 * безопасно разрешить origin по маске "*" (allowedOriginPatterns), одновременно
 * держа allowCredentials(false). Это НЕ тот антипаттерн, что был найден при
 * аудите безопасности (там был "*" ВМЕСТЕ с allowCredentials(true) — то есть
 * браузер отправлял бы cookies/credentials на разрешение с любого сайта). Здесь
 * cookies никогда не участвуют в авторизации, поэтому credentials попросту
 * отключены, а origin не проверяется вовсе — как и должно быть для API,
 * защищённого исключительно токеном в заголовке.
 *
 * Почему не белый список конкретных хостов: приложение разворачивается через
 * Docker Compose + nginx на разных машинах (localhost при локальном запуске,
 * реальное имя корпоративного Windows-сервера в продакшене) — заранее вписать
 * все возможные origin'ы невозможно, а привязка к "localhost" ломает боевое
 * окружение (именно так и произошла регрессия: POST /api/auth/login браузер
 * отправляет с заголовком Origin, curl — без него; жёсткий белый список
 * пропускал curl, но резал реальные запросы браузера).
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

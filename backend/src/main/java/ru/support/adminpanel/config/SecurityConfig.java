package ru.support.adminpanel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.support.adminpanel.security.JwtAuthFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Без явного entry point Spring Security по умолчанию отвечает 403 Forbidden
            // на запрос без валидной аутентификации (отсутствующий/просроченный/некорректный
            // JWT), а не 401 Unauthorized. Фронтенд (см. api/client.js) реагирует именно на
            // 401 — перехватывает его, чистит токен и перенаправляет на /login. Из-за 403
            // вместо 401 при протухшем токене страница вместо редиректа на логин зависала
            // на "Загрузка..." (запрос падал, а обработчик ошибки не был настроен). Поэтому
            // здесь явно возвращаем 401 с телом в общем формате ошибок ({"error": "..."}).
            .exceptionHandling(handling -> handling.authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(401);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\":\"Требуется авторизация — войдите заново\"}");
            }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/scripts", "/api/scripts/*/execute").authenticated()
                .requestMatchers("/api/scripts/**").hasRole("ADMIN")
                .requestMatchers("/api/system/log-level").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

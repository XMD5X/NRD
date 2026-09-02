package ru.support.adminpanel.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Отметка "последний раз замечен" по каждому авторизованному пользователю —
 * только в памяти процесса backend, без записи в БД (это не аудит, а
 * сиюминутная индикация "кто сейчас за компьютером", терять её при
 * перезапуске backend совершенно нормально).
 *
 * Обновляется в {@link ru.support.adminpanel.security.JwtAuthFilter} на
 * КАЖДЫЙ успешно авторизованный запрос — то есть отражает реальную
 * активность (открытые вкладки с фоновым поллингом и т.п. считаются тоже),
 * а не только момент входа. Пользователь считается "онлайн", если последний
 * его запрос был не более {@link #ONLINE_WINDOW} назад.
 */
@Service
public class OnlineUsersTracker {

    private static final Duration ONLINE_WINDOW = Duration.ofMinutes(5);

    private final Map<UUID, Instant> lastSeen = new ConcurrentHashMap<>();

    public void touch(UUID userId) {
        if (userId != null) {
            lastSeen.put(userId, Instant.now());
        }
    }

    public boolean isOnline(UUID userId) {
        Instant seen = lastSeen.get(userId);
        return seen != null && seen.isAfter(Instant.now().minus(ONLINE_WINDOW));
    }
}

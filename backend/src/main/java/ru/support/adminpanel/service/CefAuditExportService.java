package ru.support.adminpanel.service;

import org.springframework.stereotype.Service;
import ru.support.adminpanel.entity.ActionHistory;
import ru.support.adminpanel.entity.LoginHistory;
import ru.support.adminpanel.entity.User;
import ru.support.adminpanel.repository.ActionHistoryRepository;
import ru.support.adminpanel.repository.LoginHistoryRepository;
import ru.support.adminpanel.repository.UserRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Выгрузка журнала безопасности (попытки входа + действия пользователей) в
 * формате CEF (Common Event Format, ArcSight) — для приёма во внешний SIEM.
 * Формат строки: CEF:0|Vendor|Product|Version|SignatureID|Name|Severity|Extension
 * (см. спецификацию Micro Focus ArcSight CEF).
 *
 * Источники: LoginHistory (кто и когда пытался войти, успешно/нет) и
 * ActionHistory (что администратор/пользователь сделал в системе — создание
 * и блокировка пользователей, загрузка скриптов, выполнение задач и т.п.).
 * Обе таблицы уже велись и до этой выгрузки — здесь только форматирование.
 */
@Service
public class CefAuditExportService {

    private static final String CEF_VENDOR = "SIBUR";
    private static final String CEF_PRODUCT = "NRD-Business-Panel";
    private static final String CEF_VERSION = "1.0";

    // Один из форматов даты/времени, явно разрешённых спецификацией CEF для
    // поля rt (наравне с epoch-миллисекундами) — выбран этот, а не миллисекунды,
    // чтобы дата и время были видны сразу при открытии файла глазами, а не только
    // после разбора числа парсером SIEM.
    private static final DateTimeFormatter CEF_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm:ss.SSS 'UTC'", Locale.US);

    private final LoginHistoryRepository loginHistoryRepository;
    private final ActionHistoryRepository actionHistoryRepository;
    private final UserRepository userRepository;

    public CefAuditExportService(LoginHistoryRepository loginHistoryRepository,
                                  ActionHistoryRepository actionHistoryRepository,
                                  UserRepository userRepository) {
        this.loginHistoryRepository = loginHistoryRepository;
        this.actionHistoryRepository = actionHistoryRepository;
        this.userRepository = userRepository;
    }

    public String buildCefLog() {
        Map<UUID, String> loginById = new HashMap<>();
        for (User u : userRepository.findAll()) {
            loginById.put(u.getId(), u.getLogin());
        }

        List<Event> events = new ArrayList<>();

        for (LoginHistory h : loginHistoryRepository.findAllByOrderByAttemptedAtAsc()) {
            events.add(new Event(h.getAttemptedAt(), toCef(h)));
        }
        for (ActionHistory h : actionHistoryRepository.findAllByOrderByCreatedAtAsc()) {
            events.add(new Event(h.getCreatedAt(), toCef(h, loginById)));
        }
        events.sort((a, b) -> a.time().compareTo(b.time()));

        StringBuilder sb = new StringBuilder();
        for (Event e : events) {
            sb.append(e.line()).append('\n');
        }
        return sb.toString();
    }

    private String toCef(LoginHistory h) {
        String sigId = h.isSuccess() ? "LOGIN_SUCCESS" : "LOGIN_FAILURE";
        String name = h.isSuccess() ? "Успешный вход в систему" : "Неудачная попытка входа";
        int severity = h.isSuccess() ? 2 : 6;
        String extension = "rt=" + formatCefTime(h.getAttemptedAt())
                + " suser=" + escapeExtension(h.getLoginAttempted())
                + " src=" + escapeExtension(h.getIpAddress() == null ? "unknown" : h.getIpAddress())
                + " outcome=" + (h.isSuccess() ? "Success" : "Failure");
        return header(sigId, name, severity) + extension;
    }

    private String toCef(ActionHistory h, Map<UUID, String> loginById) {
        String actor = h.getUserId() == null ? "unknown" : loginById.getOrDefault(h.getUserId(), h.getUserId().toString());
        StringBuilder extension = new StringBuilder();
        extension.append("rt=").append(formatCefTime(h.getCreatedAt()))
                .append(" suser=").append(escapeExtension(actor))
                .append(" act=").append(escapeExtension(h.getActionType()))
                .append(" outcome=Success");
        if (h.getEntityType() != null) {
            extension.append(" cs1Label=EntityType cs1=").append(escapeExtension(h.getEntityType()));
        }
        if (h.getEntityId() != null) {
            extension.append(" cs2Label=EntityId cs2=").append(escapeExtension(h.getEntityId().toString()));
        }
        if (h.getDetails() != null && !h.getDetails().isBlank()) {
            extension.append(" msg=").append(escapeExtension(h.getDetails()));
        }
        // Действия над учётными записями и скриптами чувствительнее рядовых
        // просмотров/выполнений — отмечаем повышенной серьёзностью, чтобы SIEM
        // мог настроить на них отдельные правила корреляции.
        boolean sensitive = h.getActionType() != null
                && (h.getActionType().startsWith("USER_") || h.getActionType().startsWith("SCRIPT_"));
        int severity = sensitive ? 5 : 3;
        return header(h.getActionType(), h.getActionType(), severity) + extension;
    }

    private String header(String sigId, String name, int severity) {
        return "CEF:0|" + CEF_VENDOR + "|" + CEF_PRODUCT + "|" + CEF_VERSION + "|"
                + escapeHeader(sigId) + "|" + escapeHeader(name) + "|" + severity + "|";
    }

    private String formatCefTime(OffsetDateTime time) {
        if (time == null) {
            return "";
        }
        return time.withOffsetSameInstant(ZoneOffset.UTC).format(CEF_TIME_FORMAT);
    }

    /** CEF header-поля: экранируем "\" и "|" (разделитель полей заголовка). */
    private static String escapeHeader(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("|", "\\|");
    }

    /** CEF extension-поля: экранируем "\" и "=" (разделитель key=value), убираем переводы строк. */
    private static String escapeExtension(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("=", "\\=")
                .replace("\r", " ").replace("\n", " ");
    }

    private record Event(OffsetDateTime time, String line) {
    }
}

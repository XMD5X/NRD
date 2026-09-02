package ru.support.adminpanel.util;

import java.nio.file.Path;

/**
 * Безопасное построение имени файла на диске из имени, присланного клиентом
 * (MultipartFile.getOriginalFilename()). Имя файла в multipart-запросе — это
 * произвольная строка, полностью контролируемая вызывающей стороной; браузер
 * обычно кладёт туда просто "документ.xlsx", но специально сформированный
 * запрос может прислать "../../../etc/cron.d/evil" или абсолютный путь.
 * Без очистки Path.resolve() уйдёт по такому имени за пределы целевой папки —
 * то есть запись файла в произвольное место на диске (см. аудит безопасности).
 */
public final class SafeFileNames {

    private SafeFileNames() {
    }

    /** Оставляет только последний компонент пути (без каталогов) и убирает
     *  символы, недопустимые в безопасном имени файла. Пустое/отсутствующее
     *  имя заменяется на "file". */
    public static String sanitize(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename;
        // Берём последний компонент — отбрасываем всё, что похоже на путь,
        // независимо от разделителя (клиент может прислать что угодно, в т.ч.
        // Windows-style "\" при заливке не через браузер).
        int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        // Убираем ведущие точки (защита от ".." и скрытых файлов вида ".bashrc"),
        // затем — любые символы, кроме букв/цифр (в т.ч. кириллицы)/"._- "/пробела.
        name = name.replaceFirst("^\\.+", "");
        name = name.replaceAll("[^\\p{L}\\p{N}._\\- ]", "_");
        name = name.trim();
        if (name.isEmpty()) {
            name = "file";
        }
        if (name.length() > 150) {
            name = name.substring(name.length() - 150);
        }
        return name;
    }

    /** Строит путь внутри dir и на всякий случай (defense-in-depth, помимо
     *  очистки имени выше) проверяет, что итоговый путь реально остался внутри
     *  dir, а не ушёл наружу через "..", символьные ссылки и т.п. */
    public static Path resolveInside(Path dir, String storedName) {
        Path target = dir.resolve(storedName).normalize();
        Path base = dir.normalize();
        if (!target.startsWith(base)) {
            throw new IllegalArgumentException("Недопустимое имя файла");
        }
        return target;
    }
}

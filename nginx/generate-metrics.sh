#!/bin/sh
# Считает размер собранной статики фронтенда и кладёт результат в статический
# JSON-файл, который сам же nginx и отдаёт (GET /nginx-metrics.json, тот же
# origin — фронтенд читает его напрямую, без backend, см. AdminSettingsPage.jsx).
#
# Почему не через backend/Docker API: backend работает в отдельном контейнере
# и не видит файлы nginx-контейнера, а проброс docker.sock в backend ради одной
# цифры "сколько весит фронтенд" означал бы дать backend'у фактически root-доступ
# к хосту — неприемлемый компромисс после аудита безопасности (см. CorsConfig,
# docker-entrypoint.sh backend). Этот скрипт работает только внутри своего же
# контейнера, обычным `du`, без каких-либо дополнительных прав.
set -e

HTML_DIR=/usr/share/nginx/html
METRICS_FILE="$HTML_DIR/nginx-metrics.json"

# Не учитываем сам файл метрик предыдущего запуска контейнера в размере.
rm -f "$METRICS_FILE"

BYTES=$(du -sb "$HTML_DIR" 2>/dev/null | cut -f1)
if [ -z "$BYTES" ]; then
    BYTES=0
fi
GENERATED_AT=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

cat > "$METRICS_FILE" << JSON
{"staticBytes": $BYTES, "generatedAt": "$GENERATED_AT"}
JSON

echo "[generate-metrics] Размер статики фронтенда: $BYTES байт -> $METRICS_FILE"

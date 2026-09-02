#!/bin/sh
# Генерирует самоподписанный TLS-сертификат для HTTPS, если его ещё нет в
# смонтированном томе (см. docker-compose.yml: nginx_certs). Один раз
# сгенерированный сертификат переживает пересоздание контейнера (том
# сохраняется), так что при обычных перезапусках/пересборках ничего заново
# не генерируется.
#
# Для продакшена (реальный домен) вместо этого самоподписанного сертификата
# положите свои server.crt и server.key от корпоративного CA в этот же том —
# см. README.md, раздел "HTTPS".
set -e

CERT_DIR=/etc/nginx/certs
CERT_FILE="$CERT_DIR/server.crt"
KEY_FILE="$CERT_DIR/server.key"

if [ -f "$CERT_FILE" ] && [ -f "$KEY_FILE" ]; then
    echo "[generate-cert] TLS-сертификат уже есть в $CERT_DIR — пропускаю генерацию."
    exit 0
fi

echo "[generate-cert] Генерирую самоподписанный TLS-сертификат в $CERT_DIR ..."
mkdir -p "$CERT_DIR"
openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
    -keyout "$KEY_FILE" \
    -out "$CERT_FILE" \
    -subj "/C=RU/O=SIBUR/CN=adminpanel.local" \
    -addext "subjectAltName=DNS:localhost,DNS:adminpanel.local,IP:127.0.0.1"
chmod 644 "$CERT_FILE"
chmod 600 "$KEY_FILE"
echo "[generate-cert] Готово: $CERT_FILE"

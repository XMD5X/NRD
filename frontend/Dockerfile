# Раздача уже собранного frontend через nginx.
#
# Сборку (vite build) сознательно не делаем внутри Docker: на этой машине сборка
# внутри контейнера (node:20-alpine, node:22-alpine и node:22-slim — пробовали все)
# стабильно падает на "SyntaxError: Named export 'build' not found" при импорте
# esbuild из vite, хотя та же версия Node вне контейнера собирает проект без ошибок.
# Похоже на локальную особенность окружения (антивирус/сеть на этой машине —
# см. README), а не баг проекта. Поэтому здесь просто раздаём статику, уже
# собранную командой `npm ci && npm run build` в frontend/ и скопированную
# в frontend/dist-prebuilt (см. build_and_export.bat / инструкцию в README —
# пересобрать заново после изменений в frontend/src нужно так же вручную).
FROM nginx:1.27-alpine

# openssl нужен только для генерации самоподписанного TLS-сертификата при
# первом запуске (см. nginx/generate-cert.sh) — библиотеки самого TLS в nginx
# уже есть, а вот CLI-утилита openssl в этот базовый образ по умолчанию не входит.
RUN apk add --no-cache openssl

COPY frontend/dist-prebuilt /usr/share/nginx/html
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY nginx/common-locations.conf /etc/nginx/common-locations.conf

# Скрипты из /docker-entrypoint.d/ запускает штатный entrypoint образа nginx
# (/docker-entrypoint.sh) сам, по алфавиту, до старта nginx — свой ENTRYPOINT
# переопределять не нужно.
COPY nginx/generate-cert.sh /docker-entrypoint.d/40-generate-cert.sh
RUN chmod +x /docker-entrypoint.d/40-generate-cert.sh

# Тот же принцип (скрипты из /docker-entrypoint.d/ штатный entrypoint образа
# nginx запускает сам, по алфавиту) — считает размер уже скопированной статики
# при каждом старте контейнера и кладёт в /nginx-metrics.json (см. карточка
# "Системные ресурсы" на /admin/settings, AdminSettingsPage.jsx).
COPY nginx/generate-metrics.sh /docker-entrypoint.d/45-generate-metrics.sh
RUN chmod +x /docker-entrypoint.d/45-generate-metrics.sh

EXPOSE 80 443

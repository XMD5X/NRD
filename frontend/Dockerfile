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
COPY frontend/dist-prebuilt /usr/share/nginx/html
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80

# Многоэтапная сборка backend.
#
# Контекст сборки — КОРЕНЬ репозитория (см. docker-compose.yml и .gitlab-ci.yml):
# после отказа от nginx отдельного образа со статикой нет, собранный фронтенд
# копируется внутрь этого образа.
#
# Именованные контексты сборки (--build-context) здесь сознательно НЕ используются:
# их понимает только BuildKit, а образы в CI собирает kaniko — он работает с одним
# контекстом. На кеш слоёв это не влияет: ключ кеша COPY считается по содержимому
# копируемых файлов, поэтому правка фронтенда не инвалидирует слой с Maven-
# зависимостями, хотя контекст и общий.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Повторные попытки и увеличенные таймауты: в корпоративной сети обращения к
# репозиторию зависимостей регулярно обрываются на середине, и сборка падает на
# случайном артефакте ("Could not transfer artifact ... (absent)"). Без retry
# каждая такая обрыв-ошибка означает полный повтор многоминутной загрузки.
ARG MVN_NET_OPTS="-Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.rto=180000 -Dmaven.wagon.http.pool=false"
COPY backend/pom.xml .
RUN mvn -q -B $MVN_NET_OPTS dependency:go-offline
COPY backend/src ./src
RUN mvn -q -B $MVN_NET_OPTS clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# python3 и bash нужны для выполнения демо-скриптов (см. sample-scripts).
# pwsh (PowerShell Core) нужен для выполнения реальных .ps1-скриптов задачи
# "Выдача прав доступа на счета для эталонных ролей" внутри Linux-контейнера backend.
RUN apt-get update && apt-get install -y --no-install-recommends python3 bash wget apt-transport-https software-properties-common \
    && wget -q https://packages.microsoft.com/config/ubuntu/22.04/packages-microsoft-prod.deb -O /tmp/packages-microsoft-prod.deb \
    && dpkg -i /tmp/packages-microsoft-prod.deb \
    && rm /tmp/packages-microsoft-prod.deb \
    && apt-get update \
    && apt-get install -y --no-install-recommends powershell \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /build/target/admin-panel-backend.jar app.jar
COPY backend/docker-entrypoint.sh /app/docker-entrypoint.sh

# Собранный фронтенд. Сборку (vite build) по-прежнему делаем вне Docker — на этой
# машине она стабильно падает внутри контейнера ("SyntaxError: Named export 'build'
# not found"), поэтому в образ кладётся готовый результат из frontend/dist-prebuilt
# (пересобирается командой `npm ci && npm run build` в frontend/, см. README).
COPY frontend/dist-prebuilt /app/static

# Непривилегированный пользователь для запуска java-процесса. Раньше backend работал
# от root — при том, что это приложение по своей природе выполняет загруженные
# администратором скрипты как есть (см. ScriptService), запуск от root означал, что
# любой баг в контроле доступа к загрузке скриптов (см. аудит безопасности) сразу
# давал атакующему root в контейнере.
RUN mkdir -p /app/data && chmod +x /app/docker-entrypoint.sh && groupadd -r appuser && useradd -r -g appuser -d /app -s /usr/sbin/nologin appuser
EXPOSE 8080
# entrypoint стартует от root только на долю секунды, чтобы поправить владельца
# тома /app/data (см. docker-entrypoint.sh), и сразу передаёт управление java
# от имени appuser — постоянно root внутри контейнера не работает.
ENTRYPOINT ["/app/docker-entrypoint.sh"]

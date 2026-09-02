# Многоэтапная сборка backend.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

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
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

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

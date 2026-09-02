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
RUN mkdir -p /app/data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

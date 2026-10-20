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

# Среды выполнения для загружаемых скриптов: python3 и bash — для демо-скриптов
# (см. sample-scripts), pwsh (PowerShell Core) — для реальных .ps1 задачи
# "Выдача прав доступа на счета для эталонных ролей".
#
# В закрытом контуре без выхода в интернет этот слой собрать нельзя. Чтобы не
# править Dockerfile руками (а именно правка руками ломает сборку), отключайте
# его аргументом:  docker build --build-arg INSTALL_SCRIPT_RUNTIMES=0 ...
# ВАЖНО: без этих пакетов приложение стартует и отдаёт интерфейс, но ЗАПУСК
# скриптов работать не будет — это годится для проверки развёртывания, но не
# для рабочего контура. Для рабочего контура соберите образ там, где есть
# доступ к репозиториям пакетов, либо используйте внутреннее зеркало.
ARG INSTALL_SCRIPT_RUNTIMES=1
RUN if [ "$INSTALL_SCRIPT_RUNTIMES" = "1" ]; then \
      apt-get update && apt-get install -y --no-install-recommends python3 bash wget apt-transport-https software-properties-common \
      && wget -q https://packages.microsoft.com/config/ubuntu/22.04/packages-microsoft-prod.deb -O /tmp/packages-microsoft-prod.deb \
      && dpkg -i /tmp/packages-microsoft-prod.deb \
      && rm /tmp/packages-microsoft-prod.deb \
      && apt-get update \
      && apt-get install -y --no-install-recommends powershell \
      && rm -rf /var/lib/apt/lists/*; \
    else \
      echo "INSTALL_SCRIPT_RUNTIMES=0 — python3/bash/pwsh не установлены, запуск скриптов работать не будет"; \
    fi

COPY --from=build /build/target/admin-panel-backend.jar app.jar

# Собранный фронтенд. Контекст сборки — корень репозитория (см. docker-compose.yml
# и .gitlab-ci.yml), поэтому путь указан от корня. Сборку (vite build) делаем вне
# Docker: на машине разработки она стабильно падает внутри контейнера, поэтому в
# образ кладётся готовый результат (`npm ci && npm run build` в frontend/, см. README).
COPY frontend/dist-prebuilt /app/static

# Непривилегированный пользователь. Приложение по своей природе выполняет
# загруженные администратором скрипты как есть (см. ScriptService), поэтому
# запуск от root означал бы, что любой баг в контроле доступа к загрузке
# скриптов сразу даёт атакующему root в контейнере.
#
# UID/GID заданы ЧИСЛАМИ и зафиксированы в USER: Kubernetes с политикой
# runAsNonRoot проверяет пользователя до старта контейнера и не умеет
# резолвить имена из /etc/passwd — с `USER appuser` под не стартует вовсе,
# причём с пустыми логами.
#
# Каталог данных создаётся и передаётся пользователю ЗДЕСЬ, на этапе сборки,
# а не в entrypoint: менять владельца в рантайме можно только root'у с
# capability CAP_CHOWN, а в кластере со строгими политиками их нет. Права
# g+rwX нужны, чтобы том, подключённый с fsGroup, оставался доступным на запись.
ARG APP_UID=10001
ARG APP_GID=10001
RUN groupadd -g ${APP_GID} appuser \
    && useradd -u ${APP_UID} -g ${APP_GID} -d /app -s /usr/sbin/nologin appuser \
    && mkdir -p /app/data /app/static \
    && chown -R ${APP_UID}:${APP_GID} /app \
    && chmod -R g+rwX /app/data
USER ${APP_UID}:${APP_GID}

EXPOSE 8080
# Запуск напрямую, без скрипта-обёртки: отдельный entrypoint нужен был только
# для chown и смены пользователя через su — и то и другое требует root.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

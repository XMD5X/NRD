# Развёртывание MVP на другом ПК из готовых образов (офлайн)

Эта папка содержит уже собранные Docker-образы приложения — на целевом
компьютере НЕ требуется ни Maven, ни Node.js, ни доступ в интернет для сборки
(только сами образы + Docker Desktop/Engine на целевой машине).

> **Важно:** образы в этой папке — снимок на момент последнего запуска
> `build_and_export.bat` и не обновляются автоматически при изменении кода.
> После значимых изменений (новые скрипты/категории, правки frontend/backend)
> нужно на исходной машине сначала задеплоить актуальную версию (`deploy.bat`),
> затем перезапустить `build_and_export.bat`, чтобы пересобрать `dist_export/`.

## Состав

- `adminpanel-backend.tar` — образ backend (Java/Spring Boot)
- `adminpanel-web.tar` — образ frontend + nginx
- `postgres-16-alpine.tar` — образ базы данных PostgreSQL
- `docker-compose.yml` — портативная конфигурация (запускает готовые образы)

## Шаги на целевом ПК

1. Установить Docker Desktop (если ещё не установлен): https://www.docker.com/products/docker-desktop/
2. Скопировать эту папку целиком на целевой компьютер.
3. Загрузить образы в локальный Docker:
   ```
   docker load -i adminpanel-backend.tar
   docker load -i adminpanel-web.tar
   docker load -i postgres-16-alpine.tar
   ```
4. Запустить стенд:
   ```
   docker compose up -d
   ```
5. Открыть в браузере: **http://localhost:18080**
   (backend отдельно, для отладки — **http://localhost:18081**; порты не 8080/8081,
   так как эти стандартные порты часто заняты другими программами)

Демо-учётки: `admin` / `admin12345` (Администратор), `business` / `business12345`
(Бизнес-пользователь).

## Остановка

```
docker compose down
```

## Примечание

Это тот же MVP, что описан в `docs/HLD.md` и `docs/REFINED_VISION.md` (см. полный
архив с исходниками). Здесь — только рантайм-артефакты для быстрого переноса,
без исходного кода.

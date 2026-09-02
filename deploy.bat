@echo off
chcp 65001 >nul
echo ============================================
echo   Admin Panel MVP - сборка и запуск (Docker)
echo ============================================
cd /d "%~dp0"

where docker >nul 2>nul
if %errorlevel% neq 0 (
    echo.
    echo [ОШИБКА] Docker не найден в PATH.
    echo Установите Docker Desktop: https://www.docker.com/products/docker-desktop/
    echo После установки перезапустите этот файл.
    pause
    exit /b 1
)

echo Собираю и запускаю контейнеры (backend, frontend/nginx, postgres)...
docker compose up --build -d

if %errorlevel% neq 0 (
    echo.
    echo [ОШИБКА] Не удалось запустить docker compose. См. вывод выше.
    pause
    exit /b 1
)

echo.
echo Готово. Панель будет доступна через 20-30 секунд (пока backend стартует):
echo   http://localhost:18080  (обычный доступ)
echo   https://localhost:18443 (HTTPS, самоподписанный сертификат — браузер спросит
echo                            подтверждение "продолжить" при первом заходе, это ожидаемо)
echo.
echo Демо-учётки: admin / admin12345 (Администратор), business / business12345 (Бизнес-пользователь)
echo.
echo Логи можно посмотреть командой:  docker compose logs -f
pause

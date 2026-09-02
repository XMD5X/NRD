@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
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

if not exist ".env" (
    echo Файл .env не найден - генерирую случайный секрет для подписи JWT-токенов
    echo ^(раньше секрет был захардкожен в docker-compose.yml и утекал в git - см. .env.example^)...
    powershell -NoProfile -Command "$b = New-Object byte[] 48; (New-Object Security.Cryptography.RNGCryptoServiceProvider).GetBytes($b); $hex = -join ($b ^| ForEach-Object { $_.ToString('x2') }); Set-Content -Path .env -Value ('APP_JWT_SECRET=' + $hex) -Encoding ascii; Add-Content -Path .env -Value 'POSTGRES_PASSWORD=adminpanel' -Encoding ascii"
    if !errorlevel! neq 0 (
        echo.
        echo [ОШИБКА] Не удалось сгенерировать .env через PowerShell.
        echo Скопируйте .env.example в .env и заполните APP_JWT_SECRET вручную ^(любая случайная строка^).
        pause
        exit /b 1
    )
    echo .env создан. Пароль БД по умолчанию не менялся ^(см. .env.example, если нужно сменить^).
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
echo   https://localhost       (HTTPS, самоподписанный сертификат — браузер спросит
echo                           подтверждение "продолжить" при первом заходе, это ожидаемо)
echo.
echo Демо-учётки: admin / admin12345 (Администратор), business / business12345 (Бизнес-пользователь)
echo.
echo Логи можно посмотреть командой:  docker compose logs -f
pause

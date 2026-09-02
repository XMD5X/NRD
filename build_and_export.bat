@echo off
chcp 65001 >nul
echo ============================================
echo   Admin Panel MVP - сборка и экспорт образов
echo   для переноса на другой ПК (без интернета/Maven/npm там)
echo ============================================
cd /d "%~dp0"

where docker >nul 2>nul
if %errorlevel% neq 0 (
    echo [ОШИБКА] Docker не найден в PATH. Установите/запустите Docker Desktop.
    pause
    exit /b 1
)

echo.
echo [1/4] Собираю образы (backend, web)...
docker compose build
if %errorlevel% neq 0 (
    echo [ОШИБКА] Сборка не удалась. См. вывод выше.
    pause
    exit /b 1
)

echo.
echo [2/4] Скачиваю образ PostgreSQL (если ещё не скачан)...
docker pull postgres:16-alpine

echo.
echo [3/4] Экспортирую образы в папку dist_export...
if not exist dist_export mkdir dist_export
docker save -o dist_export\adminpanel-backend.tar adminpanel-backend:latest
docker save -o dist_export\adminpanel-web.tar adminpanel-web:latest
docker save -o dist_export\postgres-16-alpine.tar postgres:16-alpine

echo.
echo [4/4] Копирую файлы для развёртывания на другом ПК...
copy docker-compose.portable.yml dist_export\docker-compose.yml >nul
copy README-DEPLOY-OFFLINE.md dist_export\README.md >nul

echo.
echo ============================================
echo Готово. Папка dist_export содержит всё для переноса:
echo   - adminpanel-backend.tar, adminpanel-web.tar, postgres-16-alpine.tar
echo   - docker-compose.yml (portable, без сборки из исходников)
echo   - README.md с инструкцией
echo.
echo Скопируйте папку dist_export целиком на другой ПК (флешка/сеть)
echo и следуйте README.md.
echo ============================================
pause

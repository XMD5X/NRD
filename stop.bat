@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Останавливаю контейнеры...
docker compose down
pause

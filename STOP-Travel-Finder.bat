@echo off
setlocal EnableExtensions

set "PROJECT_DIR=C:\projekty\travel-finder"
set "COMPOSE_FILE=%PROJECT_DIR%\infra\docker-compose.yml"

echo.
echo ==============================================
echo   Travel Finder - STOP
 echo ==============================================
echo.

echo [INFO] Zatrzymuje okna frontendu i backendu...
taskkill /FI "WINDOWTITLE eq Travel Finder Backend" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq Travel Finder Frontend" /T /F >nul 2>&1

echo [INFO] Zatrzymuje PostgreSQL...
cd /d "%PROJECT_DIR%"
docker compose -f "%COMPOSE_FILE%" stop postgres >nul 2>&1

echo [OK] Travel Finder zatrzymany.
echo [INFO] Docker Desktop pozostaje uruchomiony.
echo.
pause
exit /b 0

@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "PROJECT_DIR=C:\projekty\travel-finder"
set "COMPOSE_FILE=%PROJECT_DIR%\infra\docker-compose.yml"
set "CONTAINER_NAME=travel-finder-postgres"
set "POSTGRES_HOST_PORT=5434"
set "DOCKER_DESKTOP_EXE=C:\Program Files\Docker\Docker\Docker Desktop.exe"

if not exist "%PROJECT_DIR%" (
  echo [ERROR] Folder projektu nie istnieje: %PROJECT_DIR%
  pause
  exit /b 1
)

cd /d "%PROJECT_DIR%"

echo.
echo ==============================================
echo   Travel Finder - START
 echo ==============================================
echo.

call :ensureDockerDesktop || goto :fail
call :ensureDockerEngine || goto :fail
call :startPostgres || goto :fail
call :ensureFrontendDependencies || goto :fail
call :startBackend
call :startFrontend

echo.
echo [OK] Travel Finder uruchomiony.
echo [INFO] Frontend: http://localhost:5173
echo [INFO] Backend : http://localhost:8081
echo [INFO] Postgres: localhost:%POSTGRES_HOST_PORT%
echo.
pause
exit /b 0

:ensureDockerDesktop
set "DOCKER_RUNNING="
for /f "tokens=*" %%i in ('tasklist ^| find /I "Docker Desktop.exe"') do set "DOCKER_RUNNING=1"
if defined DOCKER_RUNNING (
  echo [INFO] Docker Desktop juz dziala.
  exit /b 0
)

if exist "%DOCKER_DESKTOP_EXE%" (
  echo [INFO] Uruchamiam Docker Desktop...
  start "" "%DOCKER_DESKTOP_EXE%"
  exit /b 0
)

echo [WARN] Nie znaleziono Docker Desktop w standardowej sciezce.
echo [WARN] Sprobuję kontynuowac - jesli Docker nie ruszy, uruchom go recznie.
exit /b 0

:ensureDockerEngine
echo [INFO] Czekam na Docker Engine...
set /a attempts=0
:waitDocker
set /a attempts+=1
docker info >nul 2>&1 && (
  echo [OK] Docker Engine gotowy.
  exit /b 0
)
if !attempts! GEQ 60 (
  echo [ERROR] Docker Engine nie odpowiada.
  exit /b 1
)
timeout /t 2 >nul
goto :waitDocker

:startPostgres
echo [INFO] Uruchamiam PostgreSQL przez docker compose...
docker compose -f "%COMPOSE_FILE%" up -d postgres
if errorlevel 1 (
  echo [ERROR] Nie udalo sie uruchomic kontenera PostgreSQL.
  exit /b 1
)

echo [INFO] Czekam na healthcheck kontenera...
set /a healthAttempts=0
:waitHealth
set "PG_HEALTH="
for /f "usebackq delims=" %%i in (`docker inspect -f "{{.State.Health.Status}}" %CONTAINER_NAME% 2^>nul`) do set "PG_HEALTH=%%i"
if /I "!PG_HEALTH!"=="healthy" goto :verifyPort
set /a healthAttempts+=1
if !healthAttempts! GEQ 40 (
  echo [ERROR] PostgreSQL nie osiagnal statusu healthy.
  exit /b 1
)
timeout /t 2 >nul
goto :waitHealth

:verifyPort
set "PORT_OK="
for /f "usebackq delims=" %%i in (`docker port %CONTAINER_NAME% 5432 2^>nul`) do (
  echo [INFO] Mapowanie portu: %%i
  echo %%i | findstr /C:":%POSTGRES_HOST_PORT%" >nul && set "PORT_OK=1"
)
if not defined PORT_OK (
  echo [ERROR] PostgreSQL nie jest wystawiony na localhost port %POSTGRES_HOST_PORT%.
  echo [INFO] Aktualny docker-compose powinien miec: %POSTGRES_HOST_PORT%:5432
  exit /b 1
)

echo [OK] PostgreSQL gotowy na porcie %POSTGRES_HOST_PORT%.
exit /b 0

:ensureFrontendDependencies
if exist "%PROJECT_DIR%\frontend\node_modules" (
  echo [INFO] Zaleznosci frontendu juz istnieja.
  exit /b 0
)

echo [INFO] Instaluje zaleznosci frontendu (npm install)...
cd /d "%PROJECT_DIR%\frontend"
call npm install
if errorlevel 1 (
  echo [ERROR] npm install zakonczyl sie bledem.
  exit /b 1
)
cd /d "%PROJECT_DIR%"
exit /b 0

:startBackend
tasklist /v | findstr /I /C:"Travel Finder Backend" >nul
if not errorlevel 1 (
  echo [INFO] Backend juz jest uruchomiony.
  exit /b 0
)
echo [INFO] Uruchamiam backend...
start "Travel Finder Backend" /D "%PROJECT_DIR%\backend" cmd.exe /k "title Travel Finder Backend & mvn spring-boot:run"
exit /b 0

:startFrontend
tasklist /v | findstr /I /C:"Travel Finder Frontend" >nul
if not errorlevel 1 (
  echo [INFO] Frontend juz jest uruchomiony.
  exit /b 0
)
echo [INFO] Uruchamiam frontend...
start "Travel Finder Frontend" /D "%PROJECT_DIR%\frontend" cmd.exe /k "title Travel Finder Frontend & npm run dev"
exit /b 0

:fail
echo.
echo [ERROR] Start aplikacji nie powiodl sie.
echo.
pause
exit /b 1

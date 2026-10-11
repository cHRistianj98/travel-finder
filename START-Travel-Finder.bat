@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=C:\projekty\travel-finder"
set "COMPOSE_FILE=%ROOT%\infra\docker-compose.yml"
set "STATE_DIR=%ROOT%\.travel-finder-runtime"
set "DOCKER_DESKTOP=C:\Program Files\Docker\Docker\Docker Desktop.exe"

cd /d "%ROOT%" 2>nul || goto BAD_PROJECT
if not exist "%COMPOSE_FILE%" goto BAD_COMPOSE
if not exist "%ROOT%\backend\pom.xml" goto BAD_PROJECT
if not exist "%ROOT%\frontend\package.json" goto BAD_PROJECT
if not exist "%STATE_DIR%" mkdir "%STATE_DIR%"

echo.
echo ==========================================
echo          TRAVEL FINDER - START
echo ==========================================
echo.
echo [Travel Finder] Sprawdzam Docker Engine...

docker info >nul 2>&1
if not errorlevel 1 goto DOCKER_READY

echo [Travel Finder] Uruchamiam Docker Desktop...
if not exist "%DOCKER_DESKTOP%" goto BAD_DOCKER
start "" "%DOCKER_DESKTOP%"
set /a WAITED=0
:WAIT_FOR_DOCKER
docker info >nul 2>&1
if not errorlevel 1 goto DOCKER_READY
if !WAITED! GEQ 120 goto DOCKER_TIMEOUT
timeout /t 2 /nobreak >nul
set /a WAITED+=2
goto WAIT_FOR_DOCKER

:DOCKER_READY
echo [Travel Finder] Docker Engine gotowy.
echo [Travel Finder] Uruchamiam PostgreSQL (localhost:5434)...
docker compose -f "%COMPOSE_FILE%" up -d postgres
if errorlevel 1 goto DB_ERROR

rem Czekamy na PostgreSQL tylko wtedy, gdy baza jeszcze startuje.
set /a WAITED=0
:WAIT_FOR_DB
docker exec travel-finder-postgres pg_isready -U travel_finder -d travel_finder >nul 2>&1
if not errorlevel 1 goto DB_READY
if !WAITED! GEQ 30 goto DB_TIMEOUT
timeout /t 1 /nobreak >nul
set /a WAITED+=1
goto WAIT_FOR_DB

:DB_READY
echo [Travel Finder] PostgreSQL gotowy.
if not exist "%ROOT%\frontend\node_modules\.bin\vite.cmd" goto NPM_MISSING

echo [Travel Finder] Uruchamiam backend i frontend w osobnych oknach...
call :START_BACKEND
if errorlevel 1 goto START_ERROR
call :START_FRONTEND
if errorlevel 1 goto START_ERROR

echo.
echo [OK] Uruchomiono Travel Finder.
echo      Frontend   http://localhost:5173
echo      Backend    http://localhost:8081
echo      PostgreSQL localhost:5434
echo.
exit /b 0

:START_BACKEND
set "BACKEND_PID="
if exist "%STATE_DIR%\backend.pid" set /p "BACKEND_PID="<"%STATE_DIR%\backend.pid"
if defined BACKEND_PID (
  powershell -NoProfile -Command "$p = Get-CimInstance Win32_Process -Filter 'ProcessId = !BACKEND_PID!' -ErrorAction SilentlyContinue; if ($p -and $p.Name -eq 'cmd.exe' -and $p.CommandLine -like '*spring-boot:run*') { exit 0 } else { exit 1 }" >nul 2>&1
  if not errorlevel 1 (
    echo [Travel Finder] Backend juz uruchomiony (PID !BACKEND_PID!).
    exit /b 0
  )
)
set "BACKEND_PID="
for /f "delims=" %%P in ('powershell -NoProfile -Command "$p = Start-Process -FilePath 'cmd.exe' -ArgumentList '/k', 'mvn spring-boot:run' -WorkingDirectory '%ROOT%\backend' -PassThru; $p.Id"') do set "BACKEND_PID=%%P"
if not defined BACKEND_PID exit /b 1
>"%STATE_DIR%\backend.pid" echo !BACKEND_PID!
echo [Travel Finder] Backend uruchomiony (PID !BACKEND_PID!).
exit /b 0

:START_FRONTEND
set "FRONTEND_PID="
if exist "%STATE_DIR%\frontend.pid" set /p "FRONTEND_PID="<"%STATE_DIR%\frontend.pid"
if defined FRONTEND_PID (
  powershell -NoProfile -Command "$p = Get-CimInstance Win32_Process -Filter 'ProcessId = !FRONTEND_PID!' -ErrorAction SilentlyContinue; if ($p -and $p.Name -eq 'cmd.exe' -and $p.CommandLine -like '*npm run dev*') { exit 0 } else { exit 1 }" >nul 2>&1
  if not errorlevel 1 (
    echo [Travel Finder] Frontend juz uruchomiony (PID !FRONTEND_PID!).
    exit /b 0
  )
)
set "FRONTEND_PID="
for /f "delims=" %%P in ('powershell -NoProfile -Command "$p = Start-Process -FilePath 'cmd.exe' -ArgumentList '/k', 'npm run dev' -WorkingDirectory '%ROOT%\frontend' -PassThru; $p.Id"') do set "FRONTEND_PID=%%P"
if not defined FRONTEND_PID exit /b 1
>"%STATE_DIR%\frontend.pid" echo !FRONTEND_PID!
echo [Travel Finder] Frontend uruchomiony (PID !FRONTEND_PID!).
exit /b 0

:BAD_PROJECT
echo [ERROR] Brak katalogu lub plikow projektu: %ROOT%
goto FAIL
:BAD_COMPOSE
echo [ERROR] Nie znaleziono pliku %COMPOSE_FILE%
goto FAIL
:BAD_DOCKER
echo [ERROR] Nie znaleziono Docker Desktop: %DOCKER_DESKTOP%
goto FAIL
:DOCKER_TIMEOUT
echo [ERROR] Docker Engine nie wystartowal w 120 sekund.
goto FAIL
:DB_ERROR
echo [ERROR] Nie udalo sie uruchomic kontenera PostgreSQL.
goto FAIL
:DB_TIMEOUT
echo [ERROR] PostgreSQL nie jest gotowy po 30 sekundach.
goto FAIL
:NPM_MISSING
echo [ERROR] Brakuje zaleznosci frontendu. Wykonaj jednorazowo:
echo         cd /d "%ROOT%\frontend" ^&^& npm install
goto FAIL
:START_ERROR
echo [ERROR] Nie udalo sie uruchomic jednego z procesow.
:FAIL
echo.
pause
exit /b 1

@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "REPO_ROOT=%%~fI"
set "COMPOSE_FILE=%REPO_ROOT%\docker-compose.yml"

if not exist "%COMPOSE_FILE%" (
    echo [ERROR] docker-compose.yml not found at %COMPOSE_FILE%
    exit /b 1
)

echo 🚀 Starting Story Weaver deployment...

set ACTION=%1
if "%ACTION%"=="" set ACTION=up

where docker >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Docker is not installed. Please install Docker first.
    exit /b 1
)

docker compose version >nul 2>nul
if not errorlevel 1 (
    set "COMPOSE_CMD=docker compose -f "%COMPOSE_FILE%""
) else (
    where docker-compose >nul 2>nul
    if errorlevel 1 (
        echo [ERROR] Docker Compose is not installed. Please install Docker Compose first.
        exit /b 1
    )
    set "COMPOSE_CMD=docker-compose -f "%COMPOSE_FILE%""
)

if "%ACTION%"=="up" (
    echo [INFO] Starting Story Weaver...
    call %COMPOSE_CMD% up -d
    goto :show_summary
) else if "%ACTION%"=="down" (
    echo [INFO] Stopping Story Weaver...
    call %COMPOSE_CMD% down
) else if "%ACTION%"=="restart" (
    echo [INFO] Restarting Story Weaver...
    call %COMPOSE_CMD% restart
) else if "%ACTION%"=="logs" (
    echo [INFO] Showing logs...
    call %COMPOSE_CMD% logs -f
) else if "%ACTION%"=="build" (
    echo [INFO] Building images...
    call %COMPOSE_CMD% build
) else if "%ACTION%"=="status" (
    echo [INFO] Checking service status...
    call %COMPOSE_CMD% ps
) else if "%ACTION%"=="clean" (
    echo [INFO] Cleaning up containers and volumes...
    call %COMPOSE_CMD% down -v
) else if "%ACTION%"=="update" (
    echo [INFO] Updating Story Weaver...
    git -C "%REPO_ROOT%" pull
    call %COMPOSE_CMD% down
    call %COMPOSE_CMD% build
    call %COMPOSE_CMD% up -d
) else if "%ACTION%"=="backup" (
    echo [INFO] Creating database backup...
    for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
    set timestamp=%datetime:~0,8%_%datetime:~8,6%
    docker exec story-weaver-mysql mysqldump -u root -proot story_weaver > "%REPO_ROOT%\backup_%timestamp%.sql"
) else if "%ACTION%"=="restore" (
    if "%2"=="" (
        echo [ERROR] Please specify backup file to restore
        exit /b 1
    )
    type %2 | docker exec -i story-weaver-mysql mysql -u root -proot story_weaver
) else (
    echo [ERROR] Unknown action: %ACTION%
    echo Usage: %0 {up^|down^|restart^|logs^|build^|status^|clean^|update^|backup^|restore}
    exit /b 1
)

exit /b 0

:show_summary
echo [INFO] Waiting for services to start...
timeout /t 10 /nobreak >nul
call %COMPOSE_CMD% ps
exit /b 0

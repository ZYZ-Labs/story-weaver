@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo 🚀 Starting Story Weaver deployment...

REM Parse command line arguments
set ACTION=%1
if "%ACTION%"=="" set ACTION=up

set ENV=%2
if "%ENV%"=="" set ENV=dev

REM Check if Docker is installed
where docker >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Docker is not installed. Please install Docker first.
    exit /b 1
)

REM Check if Docker Compose is installed
where docker-compose >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Docker Compose is not installed. Please install Docker Compose first.
    exit /b 1
)

if "%ACTION%"=="up" (
    echo [INFO] Starting Story Weaver in %ENV% mode...
    if "%ENV%"=="prod" (
        docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
    ) else (
        docker-compose up -d
    )
    goto :show_summary
) else if "%ACTION%"=="down" (
    echo [INFO] Stopping Story Weaver...
    docker-compose down
) else if "%ACTION%"=="restart" (
    echo [INFO] Restarting Story Weaver...
    docker-compose restart
) else if "%ACTION%"=="logs" (
    echo [INFO] Showing logs...
    docker-compose logs -f
) else if "%ACTION%"=="build" (
    echo [INFO] Building images...
    docker-compose build
) else if "%ACTION%"=="status" (
    echo [INFO] Checking service status...
    docker-compose ps
) else if "%ACTION%"=="clean" (
    echo [INFO] Cleaning up containers and volumes...
    docker-compose down -v
) else if "%ACTION%"=="update" (
    echo [INFO] Updating Story Weaver...
    git pull
    docker-compose down
    docker-compose build
    docker-compose up -d
) else if "%ACTION%"=="backup" (
    echo [INFO] Creating database backup...
    for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
    set timestamp=%datetime:~0,8%_%datetime:~8,6%
    docker exec story-weaver-mysql mysqldump -u root -proot123456 story_weaver > backup_%timestamp%.sql
    echo [INFO] Backup saved to backup_%timestamp%.sql
) else if "%ACTION%"=="restore" (
    if "%2"=="" (
        echo [ERROR] Please specify backup file to restore
        exit /b 1
    )
    echo [INFO] Restoring database from %2...
    type %2 | docker exec -i story-weaver-mysql mysql -u root -proot123456 story_weaver
) else (
    echo [ERROR] Unknown action: %ACTION%
    echo Usage: %0 {up^|down^|restart^|logs^|build^|status^|clean^|update^|backup^|restore} [env]
    echo   up       - Start services (default: dev)
    echo   down     - Stop services
    echo   restart  - Restart services
    echo   logs     - Show logs
    echo   build    - Build images
    echo   status   - Check service status
    echo   clean    - Clean up containers and volumes
    echo   update   - Update from git and restart
    echo   backup   - Create database backup
    echo   restore  - Restore database from backup file
    exit /b 1
)

exit /b 0

:show_summary
echo [INFO] Waiting for services to start...
timeout /t 10 /nobreak >nul

echo [INFO] Service status:
docker-compose ps

echo.
echo 📋 Deployment Summary:
echo ----------------------------------------
echo 🌐 Frontend URL: http://localhost:3000
echo 🔧 Backend API: http://localhost:8080
echo 🗄️  MySQL: localhost:3306
echo 🔴 Redis: localhost:6379
echo ----------------------------------------
echo [INFO] Default credentials:
echo   Username: admin
echo   Password: admin123
echo ----------------------------------------
echo [INFO] Use "scripts\deploy.bat logs" to view logs
echo.
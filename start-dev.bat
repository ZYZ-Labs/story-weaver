@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul

set "SCRIPT_DIR=%~dp0"
set "BACKEND_DIR=%SCRIPT_DIR%backend"
set "FRONTEND_DIR=%SCRIPT_DIR%front"
set "LOG_DIR=%SCRIPT_DIR%logs"
set "BACKEND_LOG=%LOG_DIR%\backend-dev.log"
set "FRONTEND_LOG=%LOG_DIR%\front-dev.log"

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo ========================================
echo Story Weaver 开发环境启动脚本
echo ========================================
echo.

echo 1. 检查环境依赖...
for %%C in (java mvn node npm) do (
    where %%C >nul 2>nul
    if errorlevel 1 (
        echo ❌ 未找到依赖: %%C
        if "%%C"=="java" echo 💡 修复建议: 安装 JDK 21+ 并确保 java 在 PATH 中
        if "%%C"=="mvn" echo 💡 修复建议: 安装 Maven 3.8+ 并确保 mvn 在 PATH 中
        if "%%C"=="node" echo 💡 修复建议: 安装 Node.js 20+
        if "%%C"=="npm" echo 💡 修复建议: 重新安装 Node.js 以恢复 npm
        exit /b 1
    )
)

echo ✅ 环境检查通过
echo.

echo 2. 检查数据库连接 (MySQL localhost:3306)...
call :check_port 127.0.0.1 3306 MySQL 5
if errorlevel 1 (
    echo 💡 修复建议: 请先启动 MySQL，或检查 application.yml 的数据库地址配置
    exit /b 1
)

where mysql >nul 2>nul
if errorlevel 1 (
    echo ⚠️ 未安装 mysql 客户端，已跳过 SQL 认证检查
    echo 💡 修复建议: 可安装 mysql 客户端以启用账号密码连通性校验
) else (
    mysql -h127.0.0.1 -uroot -proot -e "SELECT 1;" >nul 2>nul
    if errorlevel 1 (
        echo ❌ MySQL 账号连接校验失败
        echo 💡 修复建议: 检查 backend\src\main\resources\application.yml 中数据库账号密码
        exit /b 1
    )
    echo ✅ MySQL 认证检查通过
)

echo.
echo 3. 启动后端服务（日志: %BACKEND_LOG%）...
pushd "%BACKEND_DIR%"
start "" /b cmd /c "mvn spring-boot:run > \"%BACKEND_LOG%\" 2>&1"
popd

call :check_port 127.0.0.1 8080 后端服务 30
if errorlevel 1 (
    echo 💡 修复建议: 检查后端日志，确认 8080 端口未被占用且数据库配置正确
    exit /b 1
)

echo.
echo 4. 启动前端服务（日志: %FRONTEND_LOG%）...
pushd "%FRONTEND_DIR%"
start "" /b cmd /c "npm run dev -- --host 0.0.0.0 > \"%FRONTEND_LOG%\" 2>&1"
popd

call :check_port 127.0.0.1 5173 前端服务 30
if errorlevel 1 (
    echo 💡 修复建议: 检查前端日志，确认依赖已安装（npm install）且 5173 端口未被占用
    exit /b 1
)

echo.
echo ========================================
echo ✅ 启动完成！
echo 前端: http://localhost:5173
echo 后端API: http://localhost:8080/api
echo 后端日志: %BACKEND_LOG%
echo 前端日志: %FRONTEND_LOG%
echo ========================================
exit /b 0

:check_port
set "HOST=%~1"
set "PORT=%~2"
set "NAME=%~3"
set "RETRIES=%~4"
if "%RETRIES%"=="" set "RETRIES=20"

for /l %%I in (1,1,%RETRIES%) do (
    powershell -NoProfile -Command "$client = New-Object Net.Sockets.TcpClient; try {$client.Connect('%HOST%', %PORT%); exit 0} catch {exit 1} finally {$client.Close()}" >nul 2>nul
    if not errorlevel 1 (
        echo ✅ %NAME% 健康检查通过 (%HOST%:%PORT%)
        exit /b 0
    )
    timeout /t 2 /nobreak >nul
)

echo ❌ %NAME% 健康检查失败，无法连接 %HOST%:%PORT%
exit /b 1

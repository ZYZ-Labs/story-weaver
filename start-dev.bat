@echo off
echo ========================================
echo Story Weaver 开发环境启动脚本
echo ========================================
echo.

echo 1. 检查环境...
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未找到 Java，请安装 JDK 21+
    pause
    exit /b 1
)

where mysql >nul 2>nul
if %errorlevel% neq 0 (
    echo 警告: 未找到 MySQL，请确保 MySQL 服务已启动
)

where redis-server >nul 2>nul
if %errorlevel% neq 0 (
    echo 警告: 未找到 Redis，请确保 Redis 服务已启动
)

echo 2. 初始化前端子模块...
cd front/vuetify-admin
if not exist node_modules (
    echo 安装前端依赖...
    call npm install
) else (
    echo 前端依赖已安装
)
cd ../..

echo 3. 启动后端服务...
start "Story Weaver Backend" cmd /k "cd backend && mvn spring-boot:run"

echo 4. 启动前端服务...
timeout /t 10 /nobreak >nul
start "Story Weaver Frontend" cmd /k "cd front/vuetify-admin && npm run dev"

echo.
echo ========================================
echo 启动完成！
echo 后端: http://localhost:8080
echo 前端: http://localhost:5173
echo 默认账户: admin / admin123
echo ========================================
echo.
pause
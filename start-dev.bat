@echo off
chcp 65001 >nul
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

echo 2. 检查数据库...
where mysql >nul 2>nul
if %errorlevel% neq 0 (
    echo 警告: 未找到 MySQL，请确保 MySQL 服务已启动
    echo 请执行数据库初始化:
    echo mysql -u root -p < backend\src\main\resources\schema.sql
)

echo 3. 初始化前端依赖...
cd front\vuetify-admin
if not exist node_modules (
    echo 安装前端依赖...
    call npm install
) else (
    echo 前端依赖已安装
)
cd ..

echo 4. 启动后端服务...
start "Story Weaver Backend" cmd /k "cd backend && echo 正在启动后端服务... && mvn spring-boot:run"

echo 5. 等待后端启动...
timeout /t 15 /nobreak >nul

echo 6. 启动前端服务...
start "Story Weaver Frontend" cmd /k "cd vuetify-admin && echo 正在启动前端服务... && npm run dev"

echo.
echo ========================================
echo 启动完成！
echo 后端: http://localhost:8080
echo 前端: http://localhost:5173
echo 默认账户: admin / admin123
echo ========================================
echo.
echo 按任意键退出此窗口...
pause >nul
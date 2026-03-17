@echo off
echo ========================================
echo Story Weaver 开发环境启动脚本
echo ========================================
echo.

echo 1. 检查环境依赖...
echo.

echo 检查 Java 版本...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 未找到 Java，请安装 JDK 21+
    pause
    exit /b 1
)

echo 检查 Maven...
mvn -v >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 未找到 Maven，请安装 Maven 3.8+
    pause
    exit /b 1
)

echo 检查 Node.js...
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 未找到 Node.js，请安装 Node.js 20+
    pause
    exit /b 1
)

echo 检查 npm...
npm --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 未找到 npm
    pause
    exit /b 1
)

echo.
echo ✅ 环境检查通过
echo.

echo 2. 启动后端服务...
echo.
cd backend
start "Story Weaver Backend" cmd /k "mvn spring-boot:run"
echo 后端启动中... (端口: 8080)
timeout /t 5 /nobreak >nul

echo.
echo 3. 启动前端服务...
echo.
cd ..\front
start "Story Weaver Frontend" cmd /k "npm run dev"
echo 前端启动中... (端口: 5173)
timeout /t 5 /nobreak >nul

echo.
echo ========================================
echo ✅ 启动完成！
echo.
echo 访问地址:
echo 前端: http://localhost:5173
echo 后端API: http://localhost:8080/api
echo.
echo 默认账户:
echo 用户名: admin
echo 密码: admin123
echo ========================================
echo.
echo 按任意键退出...
pause >nul
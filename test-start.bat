@echo off
chcp 65001 >nul
echo ========================================
echo Story Weaver 启动脚本测试
echo ========================================
echo.

echo 1. 检查目录结构...
if exist front\vuetify-admin (
    echo   ✅ 前端目录存在
) else (
    echo   ❌ 前端目录不存在
    exit /b 1
)

if exist backend (
    echo   ✅ 后端目录存在
) else (
    echo   ❌ 后端目录不存在
    exit /b 1
)

echo.
echo 2. 检查关键文件...
if exist backend\pom.xml (
    echo   ✅ 后端 Maven 配置文件存在
) else (
    echo   ❌ 后端 Maven 配置文件不存在
)

if exist front\vuetify-admin\package.json (
    echo   ✅ 前端 package.json 存在
) else (
    echo   ❌ 前端 package.json 不存在
)

if exist backend\src\main\resources\schema.sql (
    echo   ✅ 数据库脚本存在
) else (
    echo   ❌ 数据库脚本不存在
)

echo.
echo 3. 检查环境变量...
echo   JAVA_HOME: %JAVA_HOME%
where java >nul 2>nul
if %errorlevel% equ 0 (
    echo   ✅ Java 已安装
) else (
    echo   ❌ Java 未安装
)

where mvn >nul 2>nul
if %errorlevel% equ 0 (
    echo   ✅ Maven 已安装
) else (
    echo   ❌ Maven 未安装
)

where node >nul 2>nul
if %errorlevel% equ 0 (
    echo   ✅ Node.js 已安装
) else (
    echo   ❌ Node.js 未安装
)

echo.
echo 4. 检查前端依赖...
cd front\vuetify-admin
if exist node_modules (
    echo   ✅ 前端依赖已安装
) else (
    echo   ⚠️ 前端依赖未安装，首次启动需要安装
)
cd ..

echo.
echo 5. 模拟启动流程...
echo   a) 后端启动命令: mvn spring-boot:run
echo   b) 前端启动命令: npm run dev
echo   c) 数据库初始化: mysql -u root -p < backend\src\main\resources\schema.sql

echo.
echo ========================================
echo 测试完成！
echo.
echo 启动步骤:
echo 1. 确保 MySQL 服务已启动并执行数据库初始化
echo 2. 运行 start-dev.bat 启动前后端服务
echo 3. 访问 http://localhost:5173 使用 admin/admin123 登录
echo ========================================
echo.
pause
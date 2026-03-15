@echo off
chcp 65001 >nul
echo ========================================
echo Story Weaver 数据库初始化脚本
echo ========================================
echo.

echo 1. 检查 MySQL 服务...
where mysql >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未找到 MySQL，请确保 MySQL 服务已启动
    pause
    exit /b 1
)

echo 2. 执行数据库初始化...
echo 请按提示输入 MySQL root 密码
echo.
mysql -u root -p < backend\src\main\resources\schema.sql

if %errorlevel% equ 0 (
    echo.
    echo ✅ 数据库初始化成功！
    echo.
    echo 默认账户:
    echo   用户名: admin
    echo   密码: admin123
    echo   用户名: testuser
    echo   密码: admin123
) else (
    echo.
    echo ❌ 数据库初始化失败！
)

echo.
pause
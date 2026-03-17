@echo off
echo ========================================
echo Story Weaver 数据库初始化脚本
echo ========================================
echo.

echo 1. 检查MySQL服务...
mysql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 未找到MySQL客户端，请确保MySQL已安装并添加到PATH
    pause
    exit /b 1
)

echo.
echo 2. 请输入MySQL root密码:
set /p mysql_password=密码: 
if "%mysql_password%"=="" (
    echo ❌ 密码不能为空
    pause
    exit /b 1
)

echo.
echo 3. 创建数据库和表结构...
mysql -u root -p%mysql_password% < sql\001_init_database.sql
if %errorlevel% neq 0 (
    echo ❌ 数据库创建失败，请检查MySQL服务状态和密码
    pause
    exit /b 1
)

echo.
echo 4. 插入初始数据...
mysql -u root -p%mysql_password% story_weaver < sql\002_seed_data.sql
if %errorlevel% neq 0 (
    echo ❌ 数据插入失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo ✅ 数据库初始化完成！
echo.
echo 数据库信息:
echo 名称: story_weaver
echo 字符集: utf8mb4
echo.
echo 默认账户:
echo 1. admin / admin123
echo 2. author / admin123
echo 3. testuser / admin123
echo.
echo 示例数据已创建:
echo - 3个项目
echo - 5个章节
echo - 5个人物
echo - 3个AI写作记录
echo ========================================
echo.
echo 按任意键退出...
pause >nul
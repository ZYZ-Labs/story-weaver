@echo off
setlocal enabledelayedexpansion

set "ROOT_DIR=%~dp0.."
set "SQL_DIR=%ROOT_DIR%\sql"

echo ========================================
echo Story Weaver 数据库初始化脚本
echo ========================================

echo 1. 检查MySQL客户端...
mysql --version >nul 2>&1
if errorlevel 1 (
    echo ❌ 未找到MySQL客户端，请确保MySQL已安装并添加到PATH
    exit /b 1
)

echo.
set /p MYSQL_PASSWORD=2. 请输入MySQL root密码: 
if "%MYSQL_PASSWORD%"=="" (
    echo ❌ 密码不能为空
    exit /b 1
)

echo.
echo 3. 测试数据库连接...
mysql -u root -p%MYSQL_PASSWORD% -e "SELECT 1" >nul 2>&1
if errorlevel 1 (
    echo ❌ 数据库连接失败，请检查密码和MySQL服务状态
    exit /b 1
)

call :run_step 4 001_init_database.sql
if errorlevel 1 exit /b 1

call :run_step 5 002_seed_data.sql
if errorlevel 1 exit /b 1

call :run_step 6 003_align_legacy_dev_schema.sql
if errorlevel 1 exit /b 1

call :run_step 7 004_world_setting_and_naming_config.sql
if errorlevel 1 exit /b 1

call :run_step 8 005_world_setting_association_and_character_attributes.sql
if errorlevel 1 exit /b 1

call :run_step 9 006_account_security_and_user_management.sql
if errorlevel 1 exit /b 1

call :run_step 10 007_character_reuse_and_chapter_binding.sql
if errorlevel 1 exit /b 1

call :run_step 11 008_outline_module.sql
if errorlevel 1 exit /b 1

call :run_step 12 009_ai_writing_chat_and_workflow.sql
if errorlevel 1 exit /b 1

echo.
echo ========================================
echo ✅ 数据库初始化完成！
echo 数据库: story_weaver
echo 字符集: utf8mb4
echo ========================================
exit /b 0

:run_step
set "STEP_NO=%~1"
set "SQL_FILE=%~2"

echo.
echo [!STEP_NO!] 执行 !SQL_FILE!
mysql -u root -p%MYSQL_PASSWORD% < "%SQL_DIR%\!SQL_FILE!"
if errorlevel 1 (
    echo ❌ [!STEP_NO!] 执行失败: !SQL_FILE!
    exit /b 1
)
echo ✅ [!STEP_NO!] 执行成功: !SQL_FILE!
exit /b 0

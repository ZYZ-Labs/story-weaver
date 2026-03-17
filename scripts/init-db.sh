#!/bin/bash

echo "========================================"
echo "Story Weaver 数据库初始化脚本"
echo "========================================"
echo

echo "1. 检查MySQL服务..."
if ! command -v mysql &> /dev/null; then
    echo "❌ 未找到MySQL客户端，请确保MySQL已安装并添加到PATH"
    exit 1
fi

echo
echo "2. 检查MySQL服务状态..."
if ! systemctl is-active --quiet mysql 2>/dev/null && ! service mysql status >/dev/null 2>&1; then
    echo "⚠️ MySQL服务可能未启动，请确保MySQL服务正在运行"
    read -p "继续？(y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo
echo "3. 请输入MySQL root密码:"
read -s -p "密码: " mysql_password
echo
if [ -z "$mysql_password" ]; then
    echo "❌ 密码不能为空"
    exit 1
fi

echo
echo "4. 测试数据库连接..."
if ! mysql -u root -p"$mysql_password" -e "SELECT 1" >/dev/null 2>&1; then
    echo "❌ 数据库连接失败，请检查密码和服务状态"
    exit 1
fi

echo
echo "5. 创建数据库和表结构..."
if ! mysql -u root -p"$mysql_password" < sql/001_init_database.sql; then
    echo "❌ 数据库创建失败"
    exit 1
fi

echo
echo "6. 插入初始数据..."
if ! mysql -u root -p"$mysql_password" story_weaver < sql/002_seed_data.sql; then
    echo "❌ 数据插入失败"
    exit 1
fi

echo
echo "========================================"
echo "✅ 数据库初始化完成！"
echo
echo "数据库信息:"
echo "名称: story_weaver"
echo "字符集: utf8mb4"
echo
echo "默认账户:"
echo "1. admin / admin123"
echo "2. author / admin123"
echo "3. testuser / admin123"
echo
echo "示例数据已创建:"
echo "- 3个项目"
echo "- 5个章节"
echo "- 5个人物"
echo "- 3个AI写作记录"
echo "========================================"
echo
echo "可以使用以下命令验证:"
echo "mysql -u root -p story_weaver -e 'SELECT name FROM project;'"
echo
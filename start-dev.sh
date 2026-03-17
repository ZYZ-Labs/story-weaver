#!/bin/bash

echo "========================================"
echo "Story Weaver 开发环境启动脚本"
echo "========================================"
echo

echo "1. 检查环境依赖..."
echo

echo "检查 Java 版本..."
if ! command -v java &> /dev/null; then
    echo "❌ 未找到 Java，请安装 JDK 21+"
    exit 1
fi

echo "检查 Maven..."
if ! command -v mvn &> /dev/null; then
    echo "❌ 未找到 Maven，请安装 Maven 3.8+"
    exit 1
fi

echo "检查 Node.js..."
if ! command -v node &> /dev/null; then
    echo "❌ 未找到 Node.js，请安装 Node.js 20+"
    exit 1
fi

echo "检查 npm..."
if ! command -v npm &> /dev/null; then
    echo "❌ 未找到 npm"
    exit 1
fi

echo
echo "✅ 环境检查通过"
echo

echo "2. 检查数据库连接..."
echo "请确保 MySQL 服务已启动"
echo "数据库配置:"
echo "  Host: localhost:3306"
echo "  Database: story_weaver"
echo "  Username: root"
echo

read -p "数据库已准备就绪？(y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "请先启动 MySQL 服务并创建数据库"
    exit 1
fi

echo "3. 启动后端服务..."
echo
cd backend
gnome-terminal -- bash -c "mvn spring-boot:run; exec bash" &
echo "后端启动中... (端口: 8080)"
sleep 5

echo
echo "4. 启动前端服务..."
echo
cd ../front/vuetify-admin
gnome-terminal -- bash -c "npm run dev; exec bash" &
echo "前端启动中... (端口: 5173)"
sleep 5

echo
echo "========================================"
echo "✅ 启动完成！"
echo
echo "访问地址:"
echo "前端: http://localhost:5173"
echo "后端API: http://localhost:8080/api"
echo
echo "默认账户:"
echo "用户名: admin"
echo "密码: admin123"
echo "========================================"
echo
echo "按 Ctrl+C 停止所有服务"
echo

# 等待用户中断
trap 'echo "正在停止服务..."; pkill -f "mvn spring-boot:run"; pkill -f "npm run dev"; exit 0' INT
while true; do
    sleep 1
done
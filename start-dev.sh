#!/bin/bash

echo "========================================"
echo "Story Weaver 开发环境启动脚本"
echo "========================================"
echo

echo "1. 检查环境..."
if ! command -v java &> /dev/null; then
    echo "错误: 未找到 Java，请安装 JDK 21+"
    exit 1
fi

if ! command -v mysql &> /dev/null; then
    echo "警告: 未找到 MySQL，请确保 MySQL 服务已启动"
fi

if ! command -v redis-server &> /dev/null; then
    echo "警告: 未找到 Redis，请确保 Redis 服务已启动"
fi

echo "2. 初始化前端子模块..."
cd front/vuetify-admin
if [ ! -d "node_modules" ]; then
    echo "安装前端依赖..."
    npm install
else
    echo "前端依赖已安装"
fi
cd ../..

echo "3. 启动后端服务..."
cd backend
mvn spring-boot:run &
BACKEND_PID=$!
cd ..

echo "等待后端启动..."
sleep 10

echo "4. 启动前端服务..."
cd front/vuetify-admin
npm run dev &
FRONTEND_PID=$!
cd ../..

echo
echo "========================================"
echo "启动完成！"
echo "后端: http://localhost:8080"
echo "前端: http://localhost:5173"
echo "默认账户: admin / admin123"
echo "========================================"
echo

trap 'echo "停止服务..."; kill $BACKEND_PID $FRONTEND_PID; exit' INT

wait
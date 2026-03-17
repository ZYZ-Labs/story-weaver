#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"
FRONTEND_DIR="$SCRIPT_DIR/front"
LOG_DIR="$SCRIPT_DIR/logs"
BACKEND_LOG="$LOG_DIR/backend-dev.log"
FRONTEND_LOG="$LOG_DIR/front-dev.log"

mkdir -p "$LOG_DIR"

BACKEND_PID=""
FRONTEND_PID=""

print_fix_suggestion() {
    echo "💡 修复建议: $1"
}

check_port() {
    local host="$1"
    local port="$2"
    local name="$3"
    local retries="${4:-20}"
    local interval="${5:-2}"

    for ((i = 1; i <= retries; i++)); do
        if (echo >"/dev/tcp/$host/$port") >/dev/null 2>&1; then
            echo "✅ $name 健康检查通过 ($host:$port)"
            return 0
        fi
        sleep "$interval"
    done

    echo "❌ $name 健康检查失败，无法连接 $host:$port"
    return 1
}

cleanup() {
    echo
    echo "正在停止服务..."
    [ -n "${BACKEND_PID:-}" ] && kill "$BACKEND_PID" >/dev/null 2>&1 || true
    [ -n "${FRONTEND_PID:-}" ] && kill "$FRONTEND_PID" >/dev/null 2>&1 || true
    wait >/dev/null 2>&1 || true
    echo "服务已停止"
}

trap cleanup INT TERM

echo "========================================"
echo "Story Weaver 开发环境启动脚本"
echo "========================================"
echo

echo "1. 检查环境依赖..."

for cmd in java mvn node npm; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "❌ 未找到依赖: $cmd"
        case "$cmd" in
            java) print_fix_suggestion "安装 JDK 21+ 并确保 java 在 PATH 中" ;;
            mvn) print_fix_suggestion "安装 Maven 3.8+ 并确保 mvn 在 PATH 中" ;;
            node) print_fix_suggestion "安装 Node.js 20+" ;;
            npm) print_fix_suggestion "重新安装 Node.js 以恢复 npm" ;;
        esac
        exit 1
    fi
done

echo "✅ 环境检查通过"
echo

echo "2. 检查数据库连接 (MySQL localhost:3306)..."
if ! check_port "127.0.0.1" "3306" "MySQL" 5 1; then
    print_fix_suggestion "请先启动 MySQL，或检查 application.yml 的数据库地址配置"
    exit 1
fi

if command -v mysql >/dev/null 2>&1; then
    if ! mysql -h127.0.0.1 -uroot -proot -e "SELECT 1;" >/dev/null 2>&1; then
        echo "❌ MySQL 账号连接校验失败"
        print_fix_suggestion "检查 backend/src/main/resources/application.yml 中数据库账号密码"
        exit 1
    fi
    echo "✅ MySQL 认证检查通过"
else
    echo "⚠️ 未安装 mysql 客户端，已跳过 SQL 认证检查"
    print_fix_suggestion "可安装 mysql 客户端以启用账号密码连通性校验"
fi

echo

echo "3. 启动后端服务（日志: $BACKEND_LOG）..."
cd "$BACKEND_DIR"
nohup mvn spring-boot:run >"$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!

if ! check_port "127.0.0.1" "8080" "后端服务" 30 2; then
    tail -n 50 "$BACKEND_LOG" || true
    print_fix_suggestion "检查后端日志，确认 8080 端口未被占用且数据库配置正确"
    cleanup
    exit 1
fi

echo

echo "4. 启动前端服务（日志: $FRONTEND_LOG）..."
cd "$FRONTEND_DIR"
nohup npm run dev -- --host 0.0.0.0 >"$FRONTEND_LOG" 2>&1 &
FRONTEND_PID=$!

if ! check_port "127.0.0.1" "5173" "前端服务" 30 2; then
    tail -n 50 "$FRONTEND_LOG" || true
    print_fix_suggestion "检查前端日志，确认依赖已安装（npm install）且 5173 端口未被占用"
    cleanup
    exit 1
fi

echo

echo "========================================"
echo "✅ 启动完成！"
echo "前端: http://localhost:5173"
echo "后端API: http://localhost:8080/api"
echo "后端日志: $BACKEND_LOG"
echo "前端日志: $FRONTEND_LOG"
echo "按 Ctrl+C 停止服务"
echo "========================================"

wait

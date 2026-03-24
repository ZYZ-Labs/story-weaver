#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/front"
LOG_DIR="$ROOT_DIR/logs"
BACKEND_LOG="$LOG_DIR/backend-dev.log"
FRONTEND_LOG="$LOG_DIR/front-dev.log"
MYSQL_HOST="192.168.5.249"
MYSQL_PORT="3306"
REDIS_HOST="192.168.5.249"
REDIS_PORT="6379"
MYSQL_USER="root"
MYSQL_PASSWORD="your-local-password"
REDIS_PASSWORD="your-local-password"
REQUIRED_NODE_VERSION="$(cat "$ROOT_DIR/.nvmrc")"

if [[ -s "${NVM_DIR:-$HOME/.nvm}/nvm.sh" ]]; then
  # shellcheck source=/dev/null
  . "${NVM_DIR:-$HOME/.nvm}/nvm.sh"
fi

mkdir -p "$LOG_DIR"

ensure_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] 未找到命令: $1"
    echo "[HINT] $2"
    exit 1
  fi
  echo "[OK] 已找到命令: $1"
}

check_port() {
  local host="$1"
  local port="$2"
  local name="$3"
  local retries="${4:-8}"

  for ((i=1;i<=retries;i++)); do
    if (echo >"/dev/tcp/$host/$port") >/dev/null 2>&1; then
      echo "[OK] $name 可连接 ($host:$port)"
      return 0
    fi
    sleep 2
  done

  echo "[ERROR] $name 无法连接 ($host:$port)"
  return 1
}

check_java_version() {
  local major
  major="$(java -version 2>&1 | awk -F[\".] '/version/ {print $2; exit}')"
  if [[ -z "$major" || "$major" -lt 21 ]]; then
    echo "[ERROR] 当前 JDK 版本不足 21"
    exit 1
  fi
  echo "[OK] JDK 版本满足要求: $major"
}

select_node_version() {
  if command -v nvm >/dev/null 2>&1; then
    echo "[OK] 检测到 nvm，尝试切换到 $REQUIRED_NODE_VERSION"
    nvm use "$REQUIRED_NODE_VERSION" >/dev/null
  else
    local major
    major="$(node -v | sed 's/^v//' | cut -d. -f1)"
    if [[ "$major" -lt 20 ]]; then
      echo "[ERROR] 当前 Node 版本不足 20，且未检测到 nvm"
      exit 1
    fi
    echo "[OK] 使用当前 Node 版本: $(node -v)"
  fi
}

echo "========================================"
echo "Story Weaver 一键开发启动"
echo "========================================"

ensure_command java "请安装 JDK 21+ 并确保 java 在 PATH 中"
ensure_command mvn "请安装 Maven 3.9+ 并确保 mvn 在 PATH 中"
ensure_command node "请安装 Node.js 20+ 或使用 nvm"
ensure_command npm "请重新安装 Node.js 以恢复 npm"

check_java_version
select_node_version

check_port "$MYSQL_HOST" "$MYSQL_PORT" "MySQL" 8
if command -v mysql >/dev/null 2>&1; then
  mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1;" >/dev/null
  echo "[OK] MySQL 账号密码可用"
else
  echo "[WARN] 未找到 mysql 客户端，跳过账号密码校验"
fi

check_port "$REDIS_HOST" "$REDIS_PORT" "Redis" 8
if command -v redis-cli >/dev/null 2>&1; then
  redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" ping >/dev/null
  echo "[OK] Redis 账号密码可用"
else
  echo "[WARN] 未找到 redis-cli，跳过密码校验"
fi

echo "[OK] 安装前端依赖"
(cd "$FRONTEND_DIR" && npm install >/dev/null)

echo "[OK] 启动后端，日志: $BACKEND_LOG"
(cd "$BACKEND_DIR" && nohup mvn spring-boot:run >"$BACKEND_LOG" 2>&1 &) 
check_port "127.0.0.1" "8080" "后端服务" 45

echo "[OK] 启动前端，日志: $FRONTEND_LOG"
(cd "$FRONTEND_DIR" && nohup npm run dev -- --host 0.0.0.0 --port 5173 >"$FRONTEND_LOG" 2>&1 &)
check_port "127.0.0.1" "5173" "前端服务" 45

echo "========================================"
echo "启动完成"
echo "前端: http://localhost:5173"
echo "后端: http://localhost:8080/api"
echo "MySQL: $MYSQL_HOST:$MYSQL_PORT"
echo "Redis: $REDIS_HOST:$REDIS_PORT"
echo "========================================"

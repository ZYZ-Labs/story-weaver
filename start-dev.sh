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
    echo "[ERROR] Missing command: $1"
    echo "[HINT] $2"
    exit 1
  fi
  echo "[OK] Found command: $1"
}

check_port() {
  local host="$1"
  local port="$2"
  local name="$3"
  local retries="${4:-8}"

  for ((i=1; i<=retries; i++)); do
    if (echo >"/dev/tcp/$host/$port") >/dev/null 2>&1; then
      echo "[OK] $name reachable at $host:$port"
      return 0
    fi
    sleep 2
  done

  echo "[ERROR] $name is not reachable at $host:$port"
  return 1
}

find_listening_pids() {
  local port="$1"

  if command -v lsof >/dev/null 2>&1; then
    lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null | sort -u
    return 0
  fi

  if command -v fuser >/dev/null 2>&1; then
    fuser -n tcp "$port" 2>/dev/null | tr ' ' '\n' | sed '/^$/d' | sort -u
    return 0
  fi

  if command -v ss >/dev/null 2>&1; then
    ss -ltnp 2>/dev/null | awk -v port=":$port" '$4 ~ port {print $NF}' | sed -n 's/.*pid=\([0-9]\+\).*/\1/p' | sort -u
    return 0
  fi

  return 0
}

wait_port_released() {
  local port="$1"
  local retries="${2:-20}"

  for ((i=1; i<=retries; i++)); do
    if [[ -z "$(find_listening_pids "$port")" ]]; then
      return 0
    fi
    sleep 0.5
  done

  echo "[ERROR] Port $port is still occupied after waiting"
  return 1
}

restart_port_processes() {
  local port="$1"
  local name="$2"
  local pids

  pids="$(find_listening_pids "$port")"
  if [[ -z "$pids" ]]; then
    echo "[INFO] No existing $name process detected on port $port"
    return 0
  fi

  while IFS= read -r pid; do
    [[ -z "$pid" ]] && continue
    echo "[INFO] Stopping existing $name process: PID $pid"
    kill -9 "$pid" 2>/dev/null || true
  done <<< "$pids"

  wait_port_released "$port"
  echo "[OK] Port $port is free"
}

check_java_version() {
  local major
  major="$(java -version 2>&1 | awk -F[\".] '/version/ {print $2; exit}')"
  if [[ -z "$major" || "$major" -lt 21 ]]; then
    echo "[ERROR] JDK 21+ is required"
    exit 1
  fi
  echo "[OK] Java version: $major"
}

select_node_version() {
  if command -v nvm >/dev/null 2>&1; then
    echo "[INFO] nvm detected, trying Node $REQUIRED_NODE_VERSION"
    nvm use "$REQUIRED_NODE_VERSION" >/dev/null || true
  fi

  local major
  major="$(node -v | sed 's/^v//' | cut -d. -f1)"
  if [[ "$major" -lt 20 ]]; then
    echo "[ERROR] Node 20+ is required"
    exit 1
  fi
  echo "[OK] Node version: $(node -v)"
}

print_lan_frontend_urls() {
  if command -v hostname >/dev/null 2>&1; then
    local ips
    ips="$(hostname -I 2>/dev/null || true)"
    for ip in $ips; do
      if [[ "$ip" != 127.* && "$ip" != 169.254.* ]]; then
        echo "Frontend LAN: http://$ip:5173"
      fi
    done
  fi
}

echo "========================================"
echo "Story Weaver Dev Startup"
echo "========================================"

ensure_command java "Install JDK 21+ and make sure java is in PATH"
ensure_command mvn "Install Maven 3.9+ and make sure mvn is in PATH"
ensure_command node "Install Node.js 20+ or use nvm"
ensure_command npm "Reinstall Node.js if npm is missing"

check_java_version
select_node_version

check_port "$MYSQL_HOST" "$MYSQL_PORT" "MySQL" 8
if command -v mysql >/dev/null 2>&1; then
  mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1;" >/dev/null
  echo "[OK] MySQL credential check passed"
else
  echo "[WARN] mysql client not found, skipping credential check"
fi

check_port "$REDIS_HOST" "$REDIS_PORT" "Redis" 8
if command -v redis-cli >/dev/null 2>&1; then
  redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" ping >/dev/null
  echo "[OK] Redis password check passed"
else
  echo "[WARN] redis-cli not found, skipping password check"
fi

echo "[INFO] Installing frontend dependencies"
(cd "$FRONTEND_DIR" && npm install >/dev/null)

echo "[INFO] Restarting backend port"
restart_port_processes "8080" "Backend"

echo "[INFO] Starting backend, log: $BACKEND_LOG"
(cd "$BACKEND_DIR" && nohup mvn spring-boot:run >"$BACKEND_LOG" 2>&1 &)
check_port "127.0.0.1" "8080" "Backend" 45

echo "[INFO] Restarting frontend port"
restart_port_processes "5173" "Frontend"

echo "[INFO] Starting frontend, log: $FRONTEND_LOG"
(cd "$FRONTEND_DIR" && nohup npm run dev >"$FRONTEND_LOG" 2>&1 &)
check_port "127.0.0.1" "5173" "Frontend" 45

echo "========================================"
echo "Startup complete"
echo "Frontend: http://localhost:5173"
echo "Backend:  http://localhost:8080/api"
print_lan_frontend_urls
echo "MySQL: $MYSQL_HOST:$MYSQL_PORT"
echo "Redis: $REDIS_HOST:$REDIS_PORT"
echo "========================================"

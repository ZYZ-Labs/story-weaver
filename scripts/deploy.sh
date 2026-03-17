#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$REPO_ROOT/docker-compose.yml"

if [ ! -f "$COMPOSE_FILE" ]; then
    echo "[ERROR] docker-compose.yml not found at $COMPOSE_FILE"
    exit 1
fi

echo "🚀 Starting Story Weaver deployment..."

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_message() { echo -e "${GREEN}[INFO]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARN]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed. Please install Docker first."
    exit 1
fi

if docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD=(docker compose -f "$COMPOSE_FILE")
elif command -v docker-compose &> /dev/null; then
    COMPOSE_CMD=(docker-compose -f "$COMPOSE_FILE")
else
    print_error "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

ACTION=${1:-"up"}
ENV=${2:-"dev"}

case $ACTION in
    "up")
        print_message "Starting Story Weaver in $ENV mode..."
        "${COMPOSE_CMD[@]}" up -d
        ;;
    "down")
        print_message "Stopping Story Weaver..."
        "${COMPOSE_CMD[@]}" down
        ;;
    "restart")
        print_message "Restarting Story Weaver..."
        "${COMPOSE_CMD[@]}" restart
        ;;
    "logs")
        print_message "Showing logs..."
        "${COMPOSE_CMD[@]}" logs -f
        ;;
    "build")
        print_message "Building images..."
        "${COMPOSE_CMD[@]}" build
        ;;
    "status")
        print_message "Checking service status..."
        "${COMPOSE_CMD[@]}" ps
        ;;
    "clean")
        print_message "Cleaning up containers and volumes..."
        "${COMPOSE_CMD[@]}" down -v
        ;;
    "update")
        print_message "Updating Story Weaver..."
        git -C "$REPO_ROOT" pull
        "${COMPOSE_CMD[@]}" down
        "${COMPOSE_CMD[@]}" build
        "${COMPOSE_CMD[@]}" up -d
        ;;
    "backup")
        print_message "Creating database backup..."
        TIMESTAMP=$(date +%Y%m%d_%H%M%S)
        docker exec story-weaver-mysql mysqldump -u root -proot story_weaver > "$REPO_ROOT/backup_${TIMESTAMP}.sql"
        print_message "Backup saved to $REPO_ROOT/backup_${TIMESTAMP}.sql"
        ;;
    "restore")
        if [ -z "${2:-}" ]; then
            print_error "Please specify backup file to restore"
            exit 1
        fi
        print_message "Restoring database from $2..."
        docker exec -i story-weaver-mysql mysql -u root -proot story_weaver < "$2"
        ;;
    *)
        print_error "Unknown action: $ACTION"
        echo "Usage: $0 {up|down|restart|logs|build|status|clean|update|backup|restore}"
        exit 1
        ;;
esac

if [ "$ACTION" = "up" ]; then
    print_message "Waiting for services to start..."
    sleep 10
    print_message "Service status:"
    "${COMPOSE_CMD[@]}" ps
fi

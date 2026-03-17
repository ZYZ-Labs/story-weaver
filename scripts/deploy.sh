#!/bin/bash

set -e

echo "🚀 Starting Story Weaver deployment..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
print_message() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Parse command line arguments
ACTION=${1:-"up"}
ENV=${2:-"dev"}

case $ACTION in
    "up")
        print_message "Starting Story Weaver in $ENV mode..."
        if [ "$ENV" = "prod" ]; then
            docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
        else
            docker-compose up -d
        fi
        ;;
    "down")
        print_message "Stopping Story Weaver..."
        docker-compose down
        ;;
    "restart")
        print_message "Restarting Story Weaver..."
        docker-compose restart
        ;;
    "logs")
        print_message "Showing logs..."
        docker-compose logs -f
        ;;
    "build")
        print_message "Building images..."
        docker-compose build
        ;;
    "status")
        print_message "Checking service status..."
        docker-compose ps
        ;;
    "clean")
        print_message "Cleaning up containers and volumes..."
        docker-compose down -v
        ;;
    "update")
        print_message "Updating Story Weaver..."
        git pull
        docker-compose down
        docker-compose build
        docker-compose up -d
        ;;
    "backup")
        print_message "Creating database backup..."
        TIMESTAMP=$(date +%Y%m%d_%H%M%S)
        docker exec story-weaver-mysql mysqldump -u root -proot123456 story_weaver > backup_${TIMESTAMP}.sql
        print_message "Backup saved to backup_${TIMESTAMP}.sql"
        ;;
    "restore")
        if [ -z "$2" ]; then
            print_error "Please specify backup file to restore"
            exit 1
        fi
        print_message "Restoring database from $2..."
        docker exec -i story-weaver-mysql mysql -u root -proot123456 story_weaver < $2
        ;;
    *)
        print_error "Unknown action: $ACTION"
        echo "Usage: $0 {up|down|restart|logs|build|status|clean|update|backup|restore} [env]"
        echo "  up       - Start services (default: dev)"
        echo "  down     - Stop services"
        echo "  restart  - Restart services"
        echo "  logs     - Show logs"
        echo "  build    - Build images"
        echo "  status   - Check service status"
        echo "  clean    - Clean up containers and volumes"
        echo "  update   - Update from git and restart"
        echo "  backup   - Create database backup"
        echo "  restore  - Restore database from backup file"
        exit 1
        ;;
esac

if [ "$ACTION" = "up" ]; then
    print_message "Waiting for services to start..."
    sleep 10
    
    print_message "Service status:"
    docker-compose ps
    
    print_message "📋 Deployment Summary:"
    echo "----------------------------------------"
    echo "🌐 Frontend URL: http://localhost:3000"
    echo "🔧 Backend API: http://localhost:8080"
    echo "🗄️  MySQL: localhost:3306"
    echo "🔴 Redis: localhost:6379"
    echo "----------------------------------------"
    print_message "Default credentials:"
    echo "  Username: admin"
    echo "  Password: admin123"
    echo "----------------------------------------"
    print_message "Use './scripts/deploy.sh logs' to view logs"
fi
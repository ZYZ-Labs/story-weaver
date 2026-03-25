#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CONFIG_DIR="$REPO_ROOT/.deploy"
CONFIG_PATH="$CONFIG_DIR/dockerhub.env"

print_info() { echo "[INFO] $1"; }
print_warn() { echo "[WARN] $1"; }
print_error() { echo "[ERROR] $1" >&2; }

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        print_error "Command not found: $1"
        exit 1
    fi
}

prompt_value() {
    local label="$1"
    local default_value="${2:-}"
    local value
    if [ -n "$default_value" ]; then
        read -r -p "$label [$default_value]: " value
    else
        read -r -p "$label: " value
    fi
    if [ -z "$value" ]; then
        value="$default_value"
    fi
    printf '%s' "$value"
}

load_config() {
    if [ -f "$CONFIG_PATH" ]; then
        # shellcheck disable=SC1090
        source "$CONFIG_PATH"
    fi
}

save_config() {
    mkdir -p "$CONFIG_DIR"
    cat >"$CONFIG_PATH" <<EOF
DOCKERHUB_NAMESPACE=$DOCKERHUB_NAMESPACE
BACKEND_IMAGE_NAME=$BACKEND_IMAGE_NAME
FRONTEND_IMAGE_NAME=$FRONTEND_IMAGE_NAME
IMAGE_TAG=$IMAGE_TAG
EOF
}

require_command docker
load_config

RECONFIGURE=false
SKIP_LOGIN=false
TAG_OVERRIDE=""

for arg in "$@"; do
    case "$arg" in
        --reconfigure)
            RECONFIGURE=true
            ;;
        --skip-login)
            SKIP_LOGIN=true
            ;;
        --tag=*)
            TAG_OVERRIDE="${arg#*=}"
            ;;
    esac
done

if [ "$RECONFIGURE" = true ] || [ -z "${DOCKERHUB_NAMESPACE:-}" ] || [ -z "${BACKEND_IMAGE_NAME:-}" ] || [ -z "${FRONTEND_IMAGE_NAME:-}" ] || [ -z "${IMAGE_TAG:-}" ]; then
    print_info "Docker Hub publish config is missing or incomplete. Starting interactive setup."
    DOCKERHUB_NAMESPACE="$(prompt_value 'Docker Hub username or org' "${DOCKERHUB_NAMESPACE:-}")"
    BACKEND_IMAGE_NAME="$(prompt_value 'Backend repository name' "${BACKEND_IMAGE_NAME:-story-weaver-backend}")"
    FRONTEND_IMAGE_NAME="$(prompt_value 'Frontend repository name' "${FRONTEND_IMAGE_NAME:-story-weaver-front}")"
    IMAGE_TAG="$(prompt_value 'Image tag' "${IMAGE_TAG:-latest}")"
    save_config
    print_info "Saved config to $CONFIG_PATH"
fi

if [ -n "$TAG_OVERRIDE" ]; then
    IMAGE_TAG="$TAG_OVERRIDE"
    save_config
fi

BACKEND_IMAGE="$DOCKERHUB_NAMESPACE/$BACKEND_IMAGE_NAME:$IMAGE_TAG"
FRONTEND_IMAGE="$DOCKERHUB_NAMESPACE/$FRONTEND_IMAGE_NAME:$IMAGE_TAG"

print_warn "Make sure the target Docker Hub repositories already exist and are private."
if [ "$SKIP_LOGIN" != true ]; then
    print_info "Running docker login"
    docker login
fi

print_info "Building backend image: $BACKEND_IMAGE"
docker build --pull -t "$BACKEND_IMAGE" -f "$REPO_ROOT/backend/Dockerfile" "$REPO_ROOT/backend"

print_info "Building frontend image: $FRONTEND_IMAGE"
docker build --pull -t "$FRONTEND_IMAGE" \
    --build-arg VITE_API_BASE_URL=/api \
    --build-arg VITE_API_PROXY_TARGET=http://localhost:8080 \
    -f "$REPO_ROOT/front/Dockerfile" "$REPO_ROOT"

print_info "Pushing backend image: $BACKEND_IMAGE"
docker push "$BACKEND_IMAGE"

print_info "Pushing frontend image: $FRONTEND_IMAGE"
docker push "$FRONTEND_IMAGE"

echo
print_info "Publish complete. Use these values on the server:"
echo "  BACKEND_IMAGE=$BACKEND_IMAGE"
echo "  FRONTEND_IMAGE=$FRONTEND_IMAGE"
echo
print_info "Server compose: $REPO_ROOT/docker-compose.server.yml"
print_info "Server env example: $REPO_ROOT/.env.server.example"

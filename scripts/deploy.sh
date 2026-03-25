#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CONFIG_DIR="$REPO_ROOT/.deploy"
CONFIG_PATH="$CONFIG_DIR/registry.env"
LEGACY_CONFIG_PATH="$CONFIG_DIR/dockerhub.env"
DEFAULT_ACR_HOST="crpi-2iicgf8z27uyvaq1.cn-hangzhou.personal.cr.aliyuncs.com"
DEFAULT_ACR_NAMESPACE="silvericekey"
DEFAULT_ACR_LOGIN_USERNAME="your-registry-login-username"

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
    value="${value%$'\r'}"
    if [ -z "$value" ]; then
        value="$default_value"
    fi
    printf '%s' "$value"
}

prompt_value_into() {
    local __var_name="$1"
    local label="$2"
    local default_value="${3:-}"
    local value
    if [ -n "$default_value" ]; then
        read -r -p "$label [$default_value]: " value
    else
        read -r -p "$label: " value
    fi
    value="${value%$'\r'}"
    if [ -z "$value" ]; then
        value="$default_value"
    fi
    printf -v "$__var_name" '%s' "$value"
}

normalize_registry_host() {
    local value="${1:-}"
    value="${value#http://}"
    value="${value#https://}"
    value="${value%/}"
    printf '%s' "$value"
}

normalize_path_segment() {
    local value="${1:-}"
    value="${value#/}"
    value="${value%/}"
    printf '%s' "$value"
}

get_config_or_default() {
    local value="${1:-}"
    local default_value="${2:-}"
    if [ -n "$value" ]; then
        printf '%s' "$value"
    else
        printf '%s' "$default_value"
    fi
}

load_config() {
    if [ -f "$CONFIG_PATH" ]; then
        # shellcheck disable=SC1090
        source "$CONFIG_PATH"
        return
    fi

    if [ -f "$LEGACY_CONFIG_PATH" ]; then
        print_warn "Found legacy config at $LEGACY_CONFIG_PATH. It will be migrated to $CONFIG_PATH after save."
        # shellcheck disable=SC1090
        source "$LEGACY_CONFIG_PATH"
        REGISTRY_PROVIDER="dockerhub"
        REGISTRY_HOST="docker.io"
        LOGIN_SERVER="docker.io"
        LOGIN_USERNAME="${DOCKERHUB_NAMESPACE:-}"
        REGISTRY_NAMESPACE="${DOCKERHUB_NAMESPACE:-}"
    fi
}

save_config() {
    mkdir -p "$CONFIG_DIR"
    cat >"$CONFIG_PATH" <<EOF
REGISTRY_PROVIDER=$REGISTRY_PROVIDER
REGISTRY_HOST=$REGISTRY_HOST
LOGIN_SERVER=$LOGIN_SERVER
LOGIN_USERNAME=$LOGIN_USERNAME
REGISTRY_NAMESPACE=$REGISTRY_NAMESPACE
BACKEND_IMAGE_NAME=$BACKEND_IMAGE_NAME
FRONTEND_IMAGE_NAME=$FRONTEND_IMAGE_NAME
IMAGE_TAG=$IMAGE_TAG
EOF
}

choose_registry_provider() {
    local current_provider="${1:-aliyun-acr}"
    local default_choice="1"
    local choice

    case "$current_provider" in
        dockerhub) default_choice="1" ;;
        aliyun-acr) default_choice="2" ;;
        custom) default_choice="3" ;;
    esac

    echo >&2
    echo "Select registry provider:" >&2
    echo "  1. Docker Hub" >&2
    echo "  2. Alibaba Cloud ACR" >&2
    echo "  3. Custom registry" >&2

    while true; do
        prompt_value_into choice 'Choice' "$default_choice"
        case "$choice" in
            1) REGISTRY_PROVIDER="dockerhub"; return ;;
            2) REGISTRY_PROVIDER="aliyun-acr"; return ;;
            3) REGISTRY_PROVIDER="custom"; return ;;
            *) echo "[WARN] Invalid choice. Please enter 1, 2, or 3." >&2 ;;
        esac
    done
}

collect_interactive_config() {
    local prompt_result=""
    choose_registry_provider "${REGISTRY_PROVIDER:-dockerhub}"

    case "$REGISTRY_PROVIDER" in
        dockerhub)
            REGISTRY_HOST="docker.io"
            LOGIN_SERVER="docker.io"
            prompt_value_into prompt_result 'Docker Hub namespace or org' "$(get_config_or_default "${REGISTRY_NAMESPACE:-}" "")"
            REGISTRY_NAMESPACE="$(normalize_path_segment "$prompt_result")"
            prompt_value_into prompt_result 'Docker Hub login username' "$(get_config_or_default "${LOGIN_USERNAME:-}" "$REGISTRY_NAMESPACE")"
            LOGIN_USERNAME="$prompt_result"
            ;;
        aliyun-acr)
            prompt_value_into prompt_result 'Alibaba ACR registry host' "$(get_config_or_default "${REGISTRY_HOST:-}" "$DEFAULT_ACR_HOST")"
            REGISTRY_HOST="$(normalize_registry_host "$prompt_result")"
            prompt_value_into prompt_result 'Alibaba ACR login server' "$(get_config_or_default "${LOGIN_SERVER:-}" "$REGISTRY_HOST")"
            LOGIN_SERVER="$(normalize_registry_host "$prompt_result")"
            prompt_value_into prompt_result 'Alibaba ACR namespace' "$(get_config_or_default "${REGISTRY_NAMESPACE:-}" "$DEFAULT_ACR_NAMESPACE")"
            REGISTRY_NAMESPACE="$(normalize_path_segment "$prompt_result")"
            prompt_value_into prompt_result 'Alibaba ACR login username' "$(get_config_or_default "${LOGIN_USERNAME:-}" "$DEFAULT_ACR_LOGIN_USERNAME")"
            LOGIN_USERNAME="$prompt_result"
            ;;
        custom)
            prompt_value_into prompt_result 'Custom registry host' "$(get_config_or_default "${REGISTRY_HOST:-}" "registry.example.com")"
            REGISTRY_HOST="$(normalize_registry_host "$prompt_result")"
            prompt_value_into prompt_result 'Custom registry login server' "$(get_config_or_default "${LOGIN_SERVER:-}" "$REGISTRY_HOST")"
            LOGIN_SERVER="$(normalize_registry_host "$prompt_result")"
            prompt_value_into prompt_result 'Custom registry namespace or path segment' "$(get_config_or_default "${REGISTRY_NAMESPACE:-}" "")"
            REGISTRY_NAMESPACE="$(normalize_path_segment "$prompt_result")"
            prompt_value_into prompt_result 'Custom registry login username' "$(get_config_or_default "${LOGIN_USERNAME:-}" "")"
            LOGIN_USERNAME="$prompt_result"
            ;;
    esac

    prompt_value_into prompt_result 'Backend repository name' "$(get_config_or_default "${BACKEND_IMAGE_NAME:-}" "story-weaver-backend")"
    BACKEND_IMAGE_NAME="$(normalize_path_segment "$prompt_result")"
    prompt_value_into prompt_result 'Frontend repository name' "$(get_config_or_default "${FRONTEND_IMAGE_NAME:-}" "story-weaver-front")"
    FRONTEND_IMAGE_NAME="$(normalize_path_segment "$prompt_result")"
    prompt_value_into prompt_result 'Image tag' "$(get_config_or_default "${IMAGE_TAG:-}" "latest")"
    IMAGE_TAG="$prompt_result"
}

validate_config() {
    if [ -z "${REGISTRY_PROVIDER:-}" ] || [ -z "${BACKEND_IMAGE_NAME:-}" ] || [ -z "${FRONTEND_IMAGE_NAME:-}" ] || [ -z "${IMAGE_TAG:-}" ]; then
        return 1
    fi

    case "${REGISTRY_PROVIDER:-}" in
        dockerhub)
            [ -n "${REGISTRY_NAMESPACE:-}" ]
            ;;
        aliyun-acr)
            [ -n "${REGISTRY_HOST:-}" ] && [ -n "${REGISTRY_NAMESPACE:-}" ]
            ;;
        custom)
            [ -n "${REGISTRY_HOST:-}" ]
            ;;
        *)
            return 1
            ;;
    esac
}

build_image_reference() {
    local repository_name
    repository_name="$(normalize_path_segment "$1")"

    case "$REGISTRY_PROVIDER" in
        dockerhub)
            printf '%s' "${REGISTRY_NAMESPACE}/${repository_name}:${IMAGE_TAG}"
            ;;
        *)
            if [ -n "${REGISTRY_NAMESPACE:-}" ]; then
                printf '%s' "${REGISTRY_HOST}/${REGISTRY_NAMESPACE}/${repository_name}:${IMAGE_TAG}"
            else
                printf '%s' "${REGISTRY_HOST}/${repository_name}:${IMAGE_TAG}"
            fi
            ;;
    esac
}

run_docker_login() {
    case "$REGISTRY_PROVIDER" in
        dockerhub)
            if [ -n "${LOGIN_USERNAME:-}" ]; then
                docker login -u "$LOGIN_USERNAME"
            else
                docker login
            fi
            ;;
        *)
            if [ -n "${LOGIN_USERNAME:-}" ]; then
                docker login "${LOGIN_SERVER:-$REGISTRY_HOST}" -u "$LOGIN_USERNAME"
            else
                docker login "${LOGIN_SERVER:-$REGISTRY_HOST}"
            fi
            ;;
    esac
}

print_dockge_compose() {
    cat <<EOF
name: story-weaver

services:
  backend:
    image: $BACKEND_IMAGE
    container_name: story-weaver-backend
    restart: unless-stopped
    environment:
      TZ: Asia/Shanghai
      SPRING_DATASOURCE_URL: jdbc:mysql://192.168.5.249:3306/story_weaver?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: your-local-password
      SPRING_DATA_REDIS_HOST: 192.168.5.249
      SPRING_DATA_REDIS_PORT: 6379
      SPRING_DATA_REDIS_PASSWORD: your-local-password
      JWT_SECRET: change-this-to-a-long-random-jwt-secret-at-least-32-characters
      APP_CORS_ALLOWED_ORIGIN_PATTERNS: http://localhost:*,http://127.0.0.1:*,http://192.168.*:*,http://10.*:*
    ports:
      - "8080:8080"
    volumes:
      - xxx/story-weaver/backend/logs:/app/logs

  front:
    image: $FRONTEND_IMAGE
    container_name: story-weaver-front
    restart: unless-stopped
    depends_on:
      - backend
    environment:
      TZ: Asia/Shanghai
    ports:
      - "80:80"
    volumes:
      - xxx/story-weaver/front/nginx-logs:/var/log/nginx
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

if [ "$RECONFIGURE" = true ] || ! validate_config; then
    print_info "Registry publish config is missing or incomplete. Starting interactive setup."
    collect_interactive_config
    save_config
    print_info "Saved config to $CONFIG_PATH"
fi

if [ -n "$TAG_OVERRIDE" ]; then
    IMAGE_TAG="$TAG_OVERRIDE"
    save_config
fi

BACKEND_IMAGE="$(build_image_reference "$BACKEND_IMAGE_NAME")"
FRONTEND_IMAGE="$(build_image_reference "$FRONTEND_IMAGE_NAME")"

print_info "Registry provider: ${REGISTRY_PROVIDER}"
print_info "Backend image: $BACKEND_IMAGE"
print_info "Frontend image: $FRONTEND_IMAGE"

if [ "$SKIP_LOGIN" != true ]; then
    print_info "Running docker login"
    run_docker_login
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
echo
print_info "Dockge compose.yaml template:"
echo 'Replace every leading "xxx" in volumes with your server local path.'
echo
print_dockge_compose

param(
    [string]$Tag,
    [switch]$Reconfigure,
    [switch]$SkipLogin
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
$versionFilePath = Join-Path $repoRoot 'VERSION'
$backendPomPath = Join-Path $repoRoot 'backend\pom.xml'
$frontPackagePath = Join-Path $repoRoot 'front\package.json'
$configDir = Join-Path $repoRoot '.deploy'
$configPath = Join-Path $configDir 'registry.env'
$legacyConfigPath = Join-Path $configDir 'dockerhub.env'
$defaultAcrHost = 'crpi-2iicgf8z27uyvaq1.cn-hangzhou.personal.cr.aliyuncs.com'
$defaultAcrNamespace = 'silvericekey'
$defaultAcrLoginUsername = ''

function Write-Info([string]$Message) {
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-Warn([string]$Message) {
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-ErrorAndExit([string]$Message) {
    Write-Host "[ERROR] $Message" -ForegroundColor Red
    exit 1
}

function Test-Command([string]$CommandName) {
    return $null -ne (Get-Command $CommandName -ErrorAction SilentlyContinue)
}

function Read-EnvFile([string]$Path) {
    $result = @{}
    if (-not (Test-Path $Path)) {
        return $result
    }

    foreach ($line in Get-Content $Path) {
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }
        $trimmed = $line.Trim()
        if ($trimmed.StartsWith('#')) {
            continue
        }
        $separatorIndex = $trimmed.IndexOf('=')
        if ($separatorIndex -lt 1) {
            continue
        }
        $key = $trimmed.Substring(0, $separatorIndex).Trim()
        $value = $trimmed.Substring($separatorIndex + 1).Trim()
        $result[$key] = $value
    }

    return $result
}

function Save-EnvFile([string]$Path, [hashtable]$Values) {
    $directory = Split-Path -Parent $Path
    if (-not (Test-Path $directory)) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }

    $content = @(
        "REGISTRY_PROVIDER=$($Values.REGISTRY_PROVIDER)"
        "REGISTRY_HOST=$($Values.REGISTRY_HOST)"
        "LOGIN_SERVER=$($Values.LOGIN_SERVER)"
        "LOGIN_USERNAME=$($Values.LOGIN_USERNAME)"
        "REGISTRY_NAMESPACE=$($Values.REGISTRY_NAMESPACE)"
        "BACKEND_IMAGE_NAME=$($Values.BACKEND_IMAGE_NAME)"
        "FRONTEND_IMAGE_NAME=$($Values.FRONTEND_IMAGE_NAME)"
        "IMAGE_TAG=$($Values.IMAGE_TAG)"
    )
    Set-Content -Path $Path -Value $content -Encoding UTF8
}

function Prompt-Value([string]$Label, [string]$DefaultValue = '', [switch]$AllowEmpty) {
    $suffix = if ([string]::IsNullOrWhiteSpace($DefaultValue)) { '' } else { " [$DefaultValue]" }
    $value = Read-Host "$Label$suffix"
    if ([string]::IsNullOrWhiteSpace($value)) {
        if ($AllowEmpty.IsPresent) {
            return $DefaultValue
        }
        if (-not [string]::IsNullOrWhiteSpace($DefaultValue)) {
            return $DefaultValue
        }
        return ''
    }
    return $value.Trim()
}

function Get-ConfigOrDefault([hashtable]$Values, [string]$Key, [string]$DefaultValue = '') {
    if ($Values.ContainsKey($Key) -and -not [string]::IsNullOrWhiteSpace([string]$Values[$Key])) {
        return [string]$Values[$Key]
    }
    return $DefaultValue
}

function Get-ProjectVersion() {
    if (Test-Path $versionFilePath) {
        $version = (Get-Content -Path $versionFilePath -Raw).Trim()
        if (-not [string]::IsNullOrWhiteSpace($version)) {
            return $version
        }
    }

    if (Test-Path $backendPomPath) {
        try {
            [xml]$pom = Get-Content -Path $backendPomPath
            $version = $pom.project.version
            if (-not [string]::IsNullOrWhiteSpace($version)) {
                return $version.Trim()
            }
        } catch {
        }
    }

    if (Test-Path $frontPackagePath) {
        try {
            $package = Get-Content -Path $frontPackagePath -Raw | ConvertFrom-Json
            if (-not [string]::IsNullOrWhiteSpace($package.version)) {
                return ([string]$package.version).Trim()
            }
        } catch {
        }
    }

    return '1.0.0'
}

function Get-DefaultImageTag() {
    return (Get-ProjectVersion)
}

function Test-VersionTag([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $false
    }
    return $Value.Trim() -match '^(?:v)?\d+\.\d+\.\d+(?:[-._][0-9A-Za-z.-]+)?$'
}

function Test-ForbiddenImageTag([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $true
    }
    return $Value.Trim().ToLowerInvariant() -eq 'latest'
}

function Get-VersionTagOrDefault([string]$Candidate, [string]$Fallback) {
    if ((-not [string]::IsNullOrWhiteSpace($Candidate)) -and (Test-VersionTag $Candidate) -and (-not (Test-ForbiddenImageTag $Candidate))) {
        return $Candidate.Trim()
    }
    return $Fallback
}

function Normalize-RegistryHost([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return ''
    }
    $normalized = $Value.Trim()
    $normalized = $normalized -replace '^https?://', ''
    return $normalized.TrimEnd('/')
}

function Normalize-PathSegment([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return ''
    }
    return $Value.Trim().Trim('/')
}

function Convert-LegacyConfig([hashtable]$Values) {
    if ($Values.ContainsKey('REGISTRY_PROVIDER')) {
        return $Values
    }
    if (-not $Values.ContainsKey('DOCKERHUB_NAMESPACE')) {
        return $Values
    }

    $converted = @{}
    $converted['REGISTRY_PROVIDER'] = 'dockerhub'
    $converted['REGISTRY_HOST'] = 'docker.io'
    $converted['LOGIN_SERVER'] = 'docker.io'
    $converted['LOGIN_USERNAME'] = Get-ConfigOrDefault $Values 'DOCKERHUB_NAMESPACE' ''
    $converted['REGISTRY_NAMESPACE'] = Get-ConfigOrDefault $Values 'DOCKERHUB_NAMESPACE' ''
    $converted['BACKEND_IMAGE_NAME'] = Get-ConfigOrDefault $Values 'BACKEND_IMAGE_NAME' 'story-weaver-backend'
    $converted['FRONTEND_IMAGE_NAME'] = Get-ConfigOrDefault $Values 'FRONTEND_IMAGE_NAME' 'story-weaver-front'
    $converted['IMAGE_TAG'] = Get-VersionTagOrDefault (Get-ConfigOrDefault $Values 'IMAGE_TAG' '') (Get-DefaultImageTag)
    return $converted
}

function Load-Config() {
    if (Test-Path $configPath) {
        return Read-EnvFile $configPath
    }
    if (Test-Path $legacyConfigPath) {
        Write-Warn "Found legacy config at $legacyConfigPath. It will be migrated to $configPath after save."
        return Convert-LegacyConfig (Read-EnvFile $legacyConfigPath)
    }
    return @{}
}

function Select-RegistryProvider([string]$CurrentProvider) {
    $provider = if ([string]::IsNullOrWhiteSpace($CurrentProvider)) { 'aliyun-acr' } else { $CurrentProvider.Trim().ToLowerInvariant() }
    $defaultChoice = switch ($provider) {
        'dockerhub' { '1' }
        'aliyun-acr' { '2' }
        'custom' { '3' }
        default { '1' }
    }

    Write-Host ''
    Write-Host 'Select registry provider:'
    Write-Host '  1. Docker Hub'
    Write-Host '  2. Alibaba Cloud ACR'
    Write-Host '  3. Custom registry'

    while ($true) {
        $choice = Prompt-Value 'Choice' $defaultChoice
        switch ($choice) {
            '1' { return 'dockerhub' }
            '2' { return 'aliyun-acr' }
            '3' { return 'custom' }
            default { Write-Warn 'Invalid choice. Please enter 1, 2, or 3.' }
        }
    }
}

function Collect-InteractiveConfig([hashtable]$CurrentConfig) {
    $provider = Select-RegistryProvider (Get-ConfigOrDefault $CurrentConfig 'REGISTRY_PROVIDER' 'dockerhub')
    $defaultImageTag = Get-DefaultImageTag

    $registryHost = ''
    $loginServer = ''
    $loginUsername = Get-ConfigOrDefault $CurrentConfig 'LOGIN_USERNAME' ''
    $registryNamespace = Get-ConfigOrDefault $CurrentConfig 'REGISTRY_NAMESPACE' ''

    switch ($provider) {
        'dockerhub' {
            $registryHost = 'docker.io'
            $loginServer = 'docker.io'
            $registryNamespace = Prompt-Value 'Docker Hub namespace or org' $registryNamespace
            $loginUsername = Prompt-Value 'Docker Hub login username' (if ([string]::IsNullOrWhiteSpace($loginUsername)) { $registryNamespace } else { $loginUsername })
        }
        'aliyun-acr' {
            $registryHost = Normalize-RegistryHost (Prompt-Value 'Alibaba ACR registry host' (Get-ConfigOrDefault $CurrentConfig 'REGISTRY_HOST' $defaultAcrHost))
            $loginServer = Normalize-RegistryHost (Prompt-Value 'Alibaba ACR login server' (Get-ConfigOrDefault $CurrentConfig 'LOGIN_SERVER' $registryHost))
            $registryNamespace = Normalize-PathSegment (Prompt-Value 'Alibaba ACR namespace' (Get-ConfigOrDefault $CurrentConfig 'REGISTRY_NAMESPACE' $defaultAcrNamespace))
            $loginUsername = Prompt-Value 'Alibaba ACR login username' (Get-ConfigOrDefault $CurrentConfig 'LOGIN_USERNAME' $defaultAcrLoginUsername)
        }
        'custom' {
            $registryHost = Normalize-RegistryHost (Prompt-Value 'Custom registry host' (Get-ConfigOrDefault $CurrentConfig 'REGISTRY_HOST' 'registry.example.com'))
            $loginServer = Normalize-RegistryHost (Prompt-Value 'Custom registry login server' (Get-ConfigOrDefault $CurrentConfig 'LOGIN_SERVER' $registryHost))
            $registryNamespace = Normalize-PathSegment (Prompt-Value 'Custom registry namespace or path segment' $registryNamespace -AllowEmpty)
            $loginUsername = Prompt-Value 'Custom registry login username' $loginUsername -AllowEmpty
        }
    }

    return @{
        REGISTRY_PROVIDER = $provider
        REGISTRY_HOST = $registryHost
        LOGIN_SERVER = $loginServer
        LOGIN_USERNAME = $loginUsername
        REGISTRY_NAMESPACE = $registryNamespace
        BACKEND_IMAGE_NAME = Normalize-PathSegment (Prompt-Value 'Backend repository name' (Get-ConfigOrDefault $CurrentConfig 'BACKEND_IMAGE_NAME' 'story-weaver-backend'))
        FRONTEND_IMAGE_NAME = Normalize-PathSegment (Prompt-Value 'Frontend repository name' (Get-ConfigOrDefault $CurrentConfig 'FRONTEND_IMAGE_NAME' 'story-weaver-front'))
        IMAGE_TAG = Prompt-Value 'Image version tag' (Get-VersionTagOrDefault (Get-ConfigOrDefault $CurrentConfig 'IMAGE_TAG' '') $defaultImageTag)
    }
}

function Validate-Config([hashtable]$Config) {
    $provider = Get-ConfigOrDefault $Config 'REGISTRY_PROVIDER' ''
    if ([string]::IsNullOrWhiteSpace($provider)) {
        return $false
    }

    $requiredKeys = @('BACKEND_IMAGE_NAME', 'FRONTEND_IMAGE_NAME', 'IMAGE_TAG')
    foreach ($key in $requiredKeys) {
        if ([string]::IsNullOrWhiteSpace((Get-ConfigOrDefault $Config $key ''))) {
            return $false
        }
    }

    $imageTag = Get-ConfigOrDefault $Config 'IMAGE_TAG' ''
    if ((Test-ForbiddenImageTag $imageTag) -or (-not (Test-VersionTag $imageTag))) {
        return $false
    }

    switch ($provider) {
        'dockerhub' {
            return -not [string]::IsNullOrWhiteSpace((Get-ConfigOrDefault $Config 'REGISTRY_NAMESPACE' ''))
        }
        'aliyun-acr' {
            return (-not [string]::IsNullOrWhiteSpace((Get-ConfigOrDefault $Config 'REGISTRY_HOST' ''))) -and
                   (-not [string]::IsNullOrWhiteSpace((Get-ConfigOrDefault $Config 'REGISTRY_NAMESPACE' '')))
        }
        'custom' {
            return -not [string]::IsNullOrWhiteSpace((Get-ConfigOrDefault $Config 'REGISTRY_HOST' ''))
        }
        default {
            return $false
        }
    }
}

function Build-ImageReference([hashtable]$Config, [string]$RepositoryName) {
    $provider = Get-ConfigOrDefault $Config 'REGISTRY_PROVIDER' ''
    $registryHost = Normalize-RegistryHost (Get-ConfigOrDefault $Config 'REGISTRY_HOST' '')
    $namespace = Normalize-PathSegment (Get-ConfigOrDefault $Config 'REGISTRY_NAMESPACE' '')
    $repository = Normalize-PathSegment $RepositoryName
    $tagValue = Get-VersionTagOrDefault (Get-ConfigOrDefault $Config 'IMAGE_TAG' '') (Get-DefaultImageTag)

    if ($provider -eq 'dockerhub') {
        return "${namespace}/${repository}:${tagValue}"
    }

    if ([string]::IsNullOrWhiteSpace($namespace)) {
        return "${registryHost}/${repository}:${tagValue}"
    }

    return "${registryHost}/${namespace}/${repository}:${tagValue}"
}

function Invoke-DockerLogin([hashtable]$Config) {
    $provider = Get-ConfigOrDefault $Config 'REGISTRY_PROVIDER' 'dockerhub'
    $loginServer = Normalize-RegistryHost (Get-ConfigOrDefault $Config 'LOGIN_SERVER' '')
    $loginUsername = Get-ConfigOrDefault $Config 'LOGIN_USERNAME' ''

    if ($provider -eq 'dockerhub') {
        if ([string]::IsNullOrWhiteSpace($loginUsername)) {
            & docker login
            return
        }
        & docker login -u $loginUsername
        return
    }

    if ([string]::IsNullOrWhiteSpace($loginServer)) {
        $loginServer = Normalize-RegistryHost (Get-ConfigOrDefault $Config 'REGISTRY_HOST' '')
    }

    if ([string]::IsNullOrWhiteSpace($loginUsername)) {
        & docker login $loginServer
        return
    }

    & docker login $loginServer -u $loginUsername
}

function Write-DockgeCompose([string]$BackendImage, [string]$FrontendImage) {
    $composeTemplate = @'
name: story-weaver

services:
  backend:
    image: __BACKEND_IMAGE__
    container_name: story-weaver-backend
    restart: unless-stopped
    environment:
      TZ: Asia/Shanghai
      SPRING_DATASOURCE_URL: jdbc:mysql://192.168.5.249:3306/story_weaver?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: your-mysql-username
      SPRING_DATASOURCE_PASSWORD: your-mysql-password
      SPRING_DATA_REDIS_HOST: 192.168.5.249
      SPRING_DATA_REDIS_PORT: 6379
      SPRING_DATA_REDIS_PASSWORD: your-redis-password
      JWT_SECRET: change-this-to-a-long-random-jwt-secret-at-least-32-characters
      APP_CORS_ALLOWED_ORIGIN_PATTERNS: http://localhost:*,http://127.0.0.1:*,http://192.168.*:*,http://10.*:*,https://home.silvericekey.fun,https://home.silvericekey.fun:*
    ports:
      - "${BACKEND_PORT:-8080}:8080"
    volumes:
      - "xxx/story-weaver/backend/logs:/app/logs"

  front:
    image: __FRONTEND_IMAGE__
    container_name: story-weaver-front
    restart: unless-stopped
    environment:
      TZ: Asia/Shanghai
    expose:
      - "80"
    volumes:
      - "xxx/story-weaver/front/nginx-logs:/var/log/nginx"

  gateway:
    image: nginx:1.27-alpine
    container_name: story-weaver-gateway
    restart: unless-stopped
    depends_on:
      - front
      - backend
    environment:
      TZ: Asia/Shanghai
      SERVER_NAME: ${SERVER_NAME:-home.silvericekey.fun}
      FRONT_UPSTREAM: http://front:80
      API_UPSTREAM: http://backend:8080
      HTTPS_PORT: ${HTTPS_PORT:-443}
      SSL_CERT_FILE: ${SSL_CERT_FILE:-/etc/nginx/ssl/fullchain.pem}
      SSL_CERT_KEY_FILE: ${SSL_CERT_KEY_FILE:-/etc/nginx/ssl/privkey.pem}
    ports:
      - "${HTTP_PORT:-80}:80"
      - "${HTTPS_PORT:-443}:443"
    volumes:
      - "${SSL_CERT_DIR:-/usr/local/project/cer}:/etc/nginx/ssl:ro"
      - "xxx/story-weaver/gateway/nginx-logs:/var/log/nginx"
    command:
      - /bin/sh
      - -c
      - |
        cat >/tmp/default.conf.template <<'EOF'
        map $$http_upgrade $$connection_upgrade {
            default upgrade;
            '' close;
        }

        server {
            listen 80;
            server_name $$SERVER_NAME;

            location / {
                return 301 https://$$host$$HTTPS_PORT_SUFFIX$$request_uri;
            }
        }

        server {
            listen 443 ssl http2;
            server_name $$SERVER_NAME;

            ssl_certificate $$SSL_CERT_FILE;
            ssl_certificate_key $$SSL_CERT_KEY_FILE;
            ssl_protocols TLSv1.2 TLSv1.3;
            ssl_session_cache shared:SSL:10m;
            ssl_session_timeout 10m;

            client_max_body_size 50m;

            location /api/ {
                proxy_pass $$API_UPSTREAM;
                proxy_http_version 1.1;
                proxy_set_header Host $$host;
                proxy_set_header X-Real-IP $$remote_addr;
                proxy_set_header X-Forwarded-For $$proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Proto https;
                proxy_set_header X-Forwarded-Host $$host;
                proxy_set_header X-Forwarded-Port $$HTTPS_PORT;
                proxy_set_header Upgrade $$http_upgrade;
                proxy_set_header Connection $$connection_upgrade;
                proxy_buffering off;
                proxy_cache off;
                add_header X-Accel-Buffering "no";
                chunked_transfer_encoding on;
                proxy_connect_timeout 60s;
                proxy_send_timeout 3600s;
                proxy_read_timeout 3600s;
            }

            location / {
                proxy_pass $$FRONT_UPSTREAM;
                proxy_http_version 1.1;
                proxy_set_header Host $$host;
                proxy_set_header X-Real-IP $$remote_addr;
                proxy_set_header X-Forwarded-For $$proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Proto https;
                proxy_set_header X-Forwarded-Host $$host;
                proxy_set_header X-Forwarded-Port $$HTTPS_PORT;
                proxy_connect_timeout 60s;
                proxy_send_timeout 300s;
                proxy_read_timeout 300s;
            }
        }
        EOF
        if [ "$$HTTPS_PORT" = "443" ]; then
          export HTTPS_PORT_SUFFIX=""
        else
          export HTTPS_PORT_SUFFIX=":$$HTTPS_PORT"
        fi
        envsubst '$$SERVER_NAME $$SSL_CERT_FILE $$SSL_CERT_KEY_FILE $$FRONT_UPSTREAM $$API_UPSTREAM $$HTTPS_PORT $$HTTPS_PORT_SUFFIX' \
          < /tmp/default.conf.template > /etc/nginx/conf.d/default.conf
        exec nginx -g 'daemon off;'
'@

    $compose = $composeTemplate.Replace('__BACKEND_IMAGE__', $BackendImage).Replace('__FRONTEND_IMAGE__', $FrontendImage)

    Write-Host ''
    Write-Info 'Dockge compose.yaml template:'
    Write-Host 'Replace every leading "xxx" in log volumes with your server local path.'
    Write-Host 'SSL_CERT_DIR defaults to /usr/local/project/cer and SERVER_NAME defaults to home.silvericekey.fun.'
    Write-Host 'Change HTTP_PORT / HTTPS_PORT if you want non-default host ports.'
    Write-Host ''
    Write-Host $compose
}

if (-not (Test-Command 'docker')) {
    Write-ErrorAndExit 'docker was not found. Please install Docker first.'
}

$config = Load-Config
$defaultImageTag = Get-DefaultImageTag

if ((Test-ForbiddenImageTag (Get-ConfigOrDefault $config 'IMAGE_TAG' '')) -or [string]::IsNullOrWhiteSpace((Get-ConfigOrDefault $config 'IMAGE_TAG' ''))) {
    $config['IMAGE_TAG'] = $defaultImageTag
    if ($config.Count -gt 0) {
        Write-Warn "Image tag was missing or set to latest. Auto-migrated to version tag $defaultImageTag."
        if (Validate-Config $config) {
            Save-EnvFile -Path $configPath -Values $config
        }
    }
}

$needsSetup = $Reconfigure.IsPresent -or -not (Validate-Config $config)

if ($needsSetup) {
    Write-Info 'Registry publish config is missing or incomplete. Starting interactive setup.'
    $config = Collect-InteractiveConfig $config
    if ((Test-ForbiddenImageTag $config['IMAGE_TAG']) -or (-not (Test-VersionTag $config['IMAGE_TAG']))) {
        Write-ErrorAndExit "Image version tag must look like 1.0.0 or v1.0.0-beta.1, and cannot be latest. Current value: $($config['IMAGE_TAG'])"
    }
    Save-EnvFile -Path $configPath -Values $config
    Write-Info "Saved config to $configPath"
}

if (-not [string]::IsNullOrWhiteSpace($Tag)) {
    if ((Test-ForbiddenImageTag $Tag) -or (-not (Test-VersionTag $Tag))) {
        Write-ErrorAndExit "Tag must look like 1.0.0 or v1.0.0-beta.1, and cannot be latest. Current value: $Tag"
    }
    $config['IMAGE_TAG'] = $Tag.Trim()
    Save-EnvFile -Path $configPath -Values $config
}

$backendImage = Build-ImageReference $config (Get-ConfigOrDefault $config 'BACKEND_IMAGE_NAME' 'story-weaver-backend')
$frontendImage = Build-ImageReference $config (Get-ConfigOrDefault $config 'FRONTEND_IMAGE_NAME' 'story-weaver-front')

Write-Info "Registry provider: $(Get-ConfigOrDefault $config 'REGISTRY_PROVIDER' '')"
Write-Info "Backend image: $backendImage"
Write-Info "Frontend image: $frontendImage"

if (-not $SkipLogin.IsPresent) {
    Write-Info 'Running docker login'
    Invoke-DockerLogin $config
}

Write-Info "Building backend image: $backendImage"
docker build --pull -t $backendImage -f (Join-Path $repoRoot 'backend\Dockerfile') (Join-Path $repoRoot 'backend')

Write-Info "Building frontend image: $frontendImage"
docker build --pull -t $frontendImage --build-arg VITE_API_BASE_URL=/api --build-arg VITE_API_PROXY_TARGET=http://localhost:8080 -f (Join-Path $repoRoot 'front\Dockerfile') $repoRoot

Write-Info "Pushing backend image: $backendImage"
docker push $backendImage

Write-Info "Pushing frontend image: $frontendImage"
docker push $frontendImage

Write-Host ''
Write-Info 'Publish complete. Use these values on the server:'
Write-Host "  BACKEND_IMAGE=$backendImage"
Write-Host "  FRONTEND_IMAGE=$frontendImage"
Write-Host ''
Write-Info "Server compose: $(Join-Path $repoRoot 'docker-compose.server.yml')"
Write-Info "Server env example: $(Join-Path $repoRoot '.env.server.example')"
Write-DockgeCompose -BackendImage $backendImage -FrontendImage $frontendImage

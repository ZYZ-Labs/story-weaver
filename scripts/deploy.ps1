param(
    [string]$Tag,
    [switch]$Reconfigure,
    [switch]$SkipLogin
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
$configDir = Join-Path $repoRoot '.deploy'
$configPath = Join-Path $configDir 'registry.env'
$legacyConfigPath = Join-Path $configDir 'dockerhub.env'
$defaultAcrHost = 'crpi-2iicgf8z27uyvaq1.cn-hangzhou.personal.cr.aliyuncs.com'
$defaultAcrNamespace = 'silvericekey'
$defaultAcrLoginUsername = 'your-registry-login-username'

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
    $converted['IMAGE_TAG'] = Get-ConfigOrDefault $Values 'IMAGE_TAG' 'latest'
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
        IMAGE_TAG = Prompt-Value 'Image tag' (Get-ConfigOrDefault $CurrentConfig 'IMAGE_TAG' 'latest')
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
    $host = Normalize-RegistryHost (Get-ConfigOrDefault $Config 'REGISTRY_HOST' '')
    $namespace = Normalize-PathSegment (Get-ConfigOrDefault $Config 'REGISTRY_NAMESPACE' '')
    $repository = Normalize-PathSegment $RepositoryName
    $tagValue = Get-ConfigOrDefault $Config 'IMAGE_TAG' 'latest'

    if ($provider -eq 'dockerhub') {
        return "${namespace}/${repository}:${tagValue}"
    }

    if ([string]::IsNullOrWhiteSpace($namespace)) {
        return "${host}/${repository}:${tagValue}"
    }

    return "${host}/${namespace}/${repository}:${tagValue}"
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
    $compose = @"
name: story-weaver

services:
  backend:
    image: $BackendImage
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
    image: $FrontendImage
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
"@

    Write-Host ''
    Write-Info 'Dockge compose.yaml template:'
    Write-Host 'Replace every leading "xxx" in volumes with your server local path.'
    Write-Host ''
    Write-Host $compose
}

if (-not (Test-Command 'docker')) {
    Write-ErrorAndExit 'docker was not found. Please install Docker first.'
}

$config = Load-Config
$needsSetup = $Reconfigure.IsPresent -or -not (Validate-Config $config)

if ($needsSetup) {
    Write-Info 'Registry publish config is missing or incomplete. Starting interactive setup.'
    $config = Collect-InteractiveConfig $config
    Save-EnvFile -Path $configPath -Values $config
    Write-Info "Saved config to $configPath"
}

if (-not [string]::IsNullOrWhiteSpace($Tag)) {
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

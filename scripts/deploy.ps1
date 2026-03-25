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
$configPath = Join-Path $configDir 'dockerhub.env'

function Write-Info([string]$Message) {
    Write-Host "[INFO] $Message" -ForegroundColor Green
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
        "DOCKERHUB_NAMESPACE=$($Values.DOCKERHUB_NAMESPACE)"
        "BACKEND_IMAGE_NAME=$($Values.BACKEND_IMAGE_NAME)"
        "FRONTEND_IMAGE_NAME=$($Values.FRONTEND_IMAGE_NAME)"
        "IMAGE_TAG=$($Values.IMAGE_TAG)"
    )
    Set-Content -Path $Path -Value $content -Encoding UTF8
}

function Prompt-Value([string]$Label, [string]$DefaultValue = '') {
    $suffix = if ([string]::IsNullOrWhiteSpace($DefaultValue)) { '' } else { " [$DefaultValue]" }
    $value = Read-Host "$Label$suffix"
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $DefaultValue
    }
    return $value.Trim()
}

function Get-ConfigOrDefault([hashtable]$Values, [string]$Key, [string]$DefaultValue = '') {
    if ($Values.ContainsKey($Key) -and -not [string]::IsNullOrWhiteSpace([string]$Values[$Key])) {
        return [string]$Values[$Key]
    }
    return $DefaultValue
}

if (-not (Test-Command 'docker')) {
    Write-ErrorAndExit 'docker was not found. Please install Docker first.'
}

$config = Read-EnvFile $configPath
$requiredKeys = @('DOCKERHUB_NAMESPACE', 'BACKEND_IMAGE_NAME', 'FRONTEND_IMAGE_NAME', 'IMAGE_TAG')
$needsSetup = $Reconfigure.IsPresent
foreach ($key in $requiredKeys) {
    if (-not $config.ContainsKey($key) -or [string]::IsNullOrWhiteSpace([string]$config[$key])) {
        $needsSetup = $true
    }
}

if ($needsSetup) {
    Write-Info 'Docker Hub publish config is missing or incomplete. Starting interactive setup.'
    $config = @{
        DOCKERHUB_NAMESPACE = Prompt-Value 'Docker Hub username or org' (Get-ConfigOrDefault $config 'DOCKERHUB_NAMESPACE' '')
        BACKEND_IMAGE_NAME = Prompt-Value 'Backend repository name' (Get-ConfigOrDefault $config 'BACKEND_IMAGE_NAME' 'story-weaver-backend')
        FRONTEND_IMAGE_NAME = Prompt-Value 'Frontend repository name' (Get-ConfigOrDefault $config 'FRONTEND_IMAGE_NAME' 'story-weaver-front')
        IMAGE_TAG = Prompt-Value 'Image tag' (Get-ConfigOrDefault $config 'IMAGE_TAG' 'latest')
    }
    Save-EnvFile -Path $configPath -Values $config
    Write-Info "Saved config to $configPath"
}

if (-not [string]::IsNullOrWhiteSpace($Tag)) {
    $config['IMAGE_TAG'] = $Tag.Trim()
    Save-EnvFile -Path $configPath -Values $config
}

$namespace = [string]$config['DOCKERHUB_NAMESPACE']
$backendRepo = [string]$config['BACKEND_IMAGE_NAME']
$frontendRepo = [string]$config['FRONTEND_IMAGE_NAME']
$imageTag = [string]$config['IMAGE_TAG']

$backendImage = "${namespace}/${backendRepo}:${imageTag}"
$frontendImage = "${namespace}/${frontendRepo}:${imageTag}"

Write-Info 'Make sure the target Docker Hub repositories already exist and are private.'
if (-not $SkipLogin.IsPresent) {
    Write-Info 'Running docker login'
    docker login
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

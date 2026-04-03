$ErrorActionPreference = 'Stop'

$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = Join-Path $RootDir 'backend'
$FrontendDir = Join-Path $RootDir 'front'
$LogDir = Join-Path $RootDir 'logs'
$BackendLog = Join-Path $LogDir 'backend-dev.log'
$FrontendLog = Join-Path $LogDir 'front-dev.log'
$MysqlHost = if ($env:STORY_WEAVER_MYSQL_HOST) { $env:STORY_WEAVER_MYSQL_HOST } else { '127.0.0.1' }
$MysqlPort = if ($env:STORY_WEAVER_MYSQL_PORT) { [int]$env:STORY_WEAVER_MYSQL_PORT } else { 3306 }
$RedisHost = if ($env:STORY_WEAVER_REDIS_HOST) { $env:STORY_WEAVER_REDIS_HOST } else { '127.0.0.1' }
$RedisPort = if ($env:STORY_WEAVER_REDIS_PORT) { [int]$env:STORY_WEAVER_REDIS_PORT } else { 6379 }
$MysqlUser = if ($env:STORY_WEAVER_MYSQL_USER) { $env:STORY_WEAVER_MYSQL_USER } else { 'root' }
$MysqlPassword = if ($env:STORY_WEAVER_MYSQL_PASSWORD) { $env:STORY_WEAVER_MYSQL_PASSWORD } else { '' }
$RedisPassword = if ($env:STORY_WEAVER_REDIS_PASSWORD) { $env:STORY_WEAVER_REDIS_PASSWORD } else { '' }
$RequiredNodeVersion = '20.19.0'

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Assert-Command {
    param(
        [string]$Name,
        [string]$Hint
    )

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "[ERROR] Missing command: $Name`n[HINT] $Hint"
    }

    Write-Host "[OK] Found command: $Name"
}

function Test-TcpPort {
    param(
        [string]$TargetHost,
        [int]$Port,
        [string]$Name,
        [int]$Retries = 8
    )

    for ($i = 0; $i -lt $Retries; $i++) {
        $client = New-Object Net.Sockets.TcpClient
        try {
            $client.Connect($TargetHost, $Port)
            Write-Host "[OK] $Name reachable at ${TargetHost}:$Port"
            return
        } catch {
            Start-Sleep -Seconds 2
        } finally {
            $client.Dispose()
        }
    }

    throw "[ERROR] $Name not reachable at ${TargetHost}:$Port"
}

function Test-TcpOnce {
    param(
        [string]$TargetHost,
        [int]$Port
    )

    $client = New-Object Net.Sockets.TcpClient
    try {
        $client.Connect($TargetHost, $Port)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Get-ListeningProcessIds {
    param(
        [int]$Port
    )

    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $connections) {
        return @()
    }

    return @($connections | Select-Object -ExpandProperty OwningProcess -Unique)
}

function Wait-PortReleased {
    param(
        [int]$Port,
        [int]$Retries = 20
    )

    for ($i = 0; $i -lt $Retries; $i++) {
        if (-not (Get-ListeningProcessIds -Port $Port).Count) {
            return
        }
        Start-Sleep -Milliseconds 500
    }

    throw "[ERROR] Port $Port is still occupied after waiting"
}

function Restart-PortProcesses {
    param(
        [int]$Port,
        [string]$Name
    )

    $pids = Get-ListeningProcessIds -Port $Port
    if (-not $pids.Count) {
        Write-Host "[INFO] No existing $Name process detected on port $Port"
        return
    }

    foreach ($processId in $pids) {
        try {
            $process = Get-Process -Id $processId -ErrorAction Stop
            Write-Host "[INFO] Stopping existing $Name process: PID $processId ($($process.ProcessName))"
            Stop-Process -Id $processId -Force -ErrorAction Stop
        } catch {
            Write-Host "[WARN] Failed to stop PID $processId on port ${Port}: $($_.Exception.Message)"
        }
    }

    Wait-PortReleased -Port $Port
    Write-Host "[OK] Port $Port is free"
}

function Get-LanIPv4Addresses {
    $addresses = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object {
            $_.IPAddress -and
            $_.IPAddress -ne '127.0.0.1' -and
            -not $_.IPAddress.StartsWith('169.254.') -and
            $_.PrefixOrigin -ne 'WellKnown'
        } |
        Select-Object -ExpandProperty IPAddress -Unique

    if (-not $addresses) {
        return @()
    }

    return @($addresses)
}

Write-Host "========================================"
Write-Host "Story Weaver Dev Startup (PowerShell)"
Write-Host "========================================"

Assert-Command java 'Install JDK 21+ and add java to PATH'
Assert-Command mvn 'Install Maven 3.9+ and add mvn to PATH'
Assert-Command node 'Install Node.js 20+ or use nvm'
Assert-Command npm 'Reinstall Node.js if npm is missing'

$javaVersionText = & cmd /c "java -version 2>&1" | Select-Object -First 1
$javaMatch = [regex]::Match($javaVersionText, 'version "(?<ver>\d+)')
if (-not $javaMatch.Success -or [int]$javaMatch.Groups['ver'].Value -lt 21) {
    throw '[ERROR] JDK 21+ is required'
}
Write-Host "[OK] Java version: $($javaMatch.Groups['ver'].Value)"

if (Get-Command nvm -ErrorAction SilentlyContinue) {
    Write-Host "[INFO] nvm detected, trying Node $RequiredNodeVersion"
    try {
        & nvm use $RequiredNodeVersion | Out-Null
    } catch {
        Write-Host "[WARN] nvm could not switch to $RequiredNodeVersion"
    }
}

$actualNodeVersion = (& node -v).Trim()
if ($actualNodeVersion -eq "v$RequiredNodeVersion") {
    Write-Host "[OK] Active Node version matches .nvmrc: $actualNodeVersion"
} elseif (Get-Command nvm -ErrorAction SilentlyContinue) {
    Write-Host "[WARN] Requested Node $RequiredNodeVersion via nvm, but current Node is $actualNodeVersion"
}

$nodeMajor = [int](($actualNodeVersion -replace '^v', '').Split('.')[0])
if ($nodeMajor -lt 20) {
    throw '[ERROR] Node 20+ is required'
}
Write-Host "[OK] Node major version: $nodeMajor"

Test-TcpPort -TargetHost $MysqlHost -Port $MysqlPort -Name 'MySQL'
if (Get-Command mysql -ErrorAction SilentlyContinue) {
    $mysqlArgs = @("-h$MysqlHost", "-P$MysqlPort", "-u$MysqlUser")
    if ($MysqlPassword) {
        $mysqlArgs += "-p$MysqlPassword"
    }
    $mysqlArgs += @('-e', 'SELECT 1;')
    & mysql @mysqlArgs | Out-Null
    Write-Host '[OK] MySQL credential check passed'
} else {
    Write-Host '[WARN] mysql client not found, skipping credential check'
}

Test-TcpPort -TargetHost $RedisHost -Port $RedisPort -Name 'Redis'
if (Get-Command redis-cli -ErrorAction SilentlyContinue) {
    $redisArgs = @('-h', $RedisHost, '-p', $RedisPort)
    if ($RedisPassword) {
        $redisArgs += @('-a', $RedisPassword)
    }
    $redisArgs += 'ping'
    & redis-cli @redisArgs | Out-Null
    Write-Host '[OK] Redis password check passed'
} else {
    Write-Host '[WARN] redis-cli not found, skipping password check'
}

Write-Host '[INFO] Installing frontend dependencies'
Push-Location $FrontendDir
try {
    & npm install | Out-Null
} finally {
    Pop-Location
}

Write-Host '[INFO] Restarting backend port'
Restart-PortProcesses -Port 8080 -Name 'Backend'

Write-Host '[INFO] Starting backend'
Start-Process -FilePath 'cmd.exe' -ArgumentList "/c","mvn spring-boot:run > ""$BackendLog"" 2>&1" -WorkingDirectory $BackendDir -WindowStyle Minimized
Test-TcpPort -TargetHost '127.0.0.1' -Port 8080 -Name 'Backend' -Retries 45

Write-Host '[INFO] Restarting frontend port'
Restart-PortProcesses -Port 5173 -Name 'Frontend'

Write-Host '[INFO] Starting frontend'
Start-Process -FilePath 'cmd.exe' -ArgumentList "/c","npm run dev > ""$FrontendLog"" 2>&1" -WorkingDirectory $FrontendDir -WindowStyle Minimized
Test-TcpPort -TargetHost '127.0.0.1' -Port 5173 -Name 'Frontend' -Retries 45

Write-Host '========================================'
Write-Host 'Startup complete'
Write-Host 'Frontend: http://localhost:5173'
Write-Host 'Backend:  http://localhost:8080/api'
foreach ($ip in Get-LanIPv4Addresses) {
    Write-Host "Frontend LAN: http://${ip}:5173"
}
Write-Host "Backend log: $BackendLog"
Write-Host "Frontend log: $FrontendLog"
Write-Host '========================================'

<#
Starts the full local dev stack: postgres (docker), backend (gradlew bootRun,
local profile), miniapp (npm run dev). Reads config/secrets from the repo-root
.env file and exports them as process env vars so the backend picks them up.

Assumes a cloudflared tunnel to http://localhost:5173 is already running
separately, with its hostname set as VITE_DEV_ALLOWED_HOST / ZARFLOW_MINI_APP_URL
/ TELEGRAM_WEBHOOK_URL in .env.

Backend and miniapp run detached; logs go to .dev-logs/backend.log and
.dev-logs/miniapp.log in the repo root.
#>
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env"

if (-not (Test-Path $envFile)) {
    Write-Error ".env not found at $envFile"
    exit 1
}

$envVars = @{}
Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $idx = $line.IndexOf("=")
        $key = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        $envVars[$key] = $value
        Set-Item -Path "Env:$key" -Value $value
    }
}

if ([string]::IsNullOrWhiteSpace($envVars["TELEGRAM_BOT_TOKEN"])) {
    Write-Error "TELEGRAM_BOT_TOKEN is missing from .env -- stopping."
    exit 1
}

$env:SPRING_PROFILES_ACTIVE = "local"

$logDir = Join-Path $root ".dev-logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

Write-Host "== postgres =="
Push-Location $root
docker compose up -d postgres
Pop-Location

$timeout = 60
$elapsed = 0
while ($true) {
    $status = docker inspect -f "{{.State.Health.Status}}" zarflow-postgres-1 2>$null
    if ($status -eq "healthy") { break }
    if ($elapsed -ge $timeout) { Write-Error "postgres did not become healthy within ${timeout}s"; exit 1 }
    Start-Sleep -Seconds 2
    $elapsed += 2
}
Write-Host "postgres healthy."

Write-Host "== backend (gradlew bootRun, local profile) =="
Start-Process -FilePath (Join-Path $root "gradlew.bat") -ArgumentList "bootRun" `
    -WorkingDirectory $root -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $logDir "backend.log") `
    -RedirectStandardError (Join-Path $logDir "backend.err.log")

Write-Host "== miniapp (npm run dev) =="
Start-Process -FilePath "npm.cmd" -ArgumentList "run", "dev" `
    -WorkingDirectory (Join-Path $root "miniapp") -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $logDir "miniapp.log") `
    -RedirectStandardError (Join-Path $logDir "miniapp.err.log")

Write-Host "== waiting for backend health =="
$timeout = 120
$elapsed = 0
$healthy = $false
while ($elapsed -lt $timeout) {
    try {
        $resp = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 3
        if ($resp.status -eq "UP") { $healthy = $true; break }
    } catch {}
    Start-Sleep -Seconds 3
    $elapsed += 3
}

if (-not $healthy) {
    Write-Error "Backend did not become healthy within ${timeout}s -- check $logDir\backend.log"
    exit 1
}

Write-Host "Backend UP."
Write-Host "Mini App dev server running -- open it via your cloudflared tunnel URL: $($envVars['ZARFLOW_MINI_APP_URL'])"
Write-Host "Logs: $logDir\backend.log, $logDir\miniapp.log"

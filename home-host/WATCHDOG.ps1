$ErrorActionPreference = "SilentlyContinue"
$taskName = "Dexter AI Home Host"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$server = Join-Path $root "server.mjs"
$backup = Join-Path $root "server.mjs.last-good"

function Test-DexterHealth {
  try {
    $health = Invoke-RestMethod -Uri "http://127.0.0.1:8787/health" -TimeoutSec 5
    return ($health.status -eq "ready")
  } catch { return $false }
}

if (Test-DexterHealth) {
  Write-Host "Dexter AI is healthy."
  exit 0
}

Write-Host "Dexter AI is unhealthy. Recovering..."

# Repair a broken local server file from the last known-good copy before restart.
if (Test-Path $server) {
  & node --check $server *> $null
  if ($LASTEXITCODE -ne 0 -and (Test-Path $backup)) {
    Copy-Item -Force $backup $server
    Write-Host "Restored last known-good server.mjs."
  }
}

$task = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if (-not $task) {
  $register = Join-Path $root "REGISTER-AUTOSTART.ps1"
  if (Test-Path $register) {
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File $register
    exit $LASTEXITCODE
  }
  exit 1
}

try { Stop-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue } catch {}
Start-Sleep -Seconds 2
try {
  Start-ScheduledTask -TaskName $taskName
  Start-Sleep -Seconds 8
  if (Test-DexterHealth) {
    Write-Host "Dexter AI recovered."
    exit 0
  }
  Write-Host "Dexter AI restart requested; health check still pending."
  exit 1
} catch {
  Write-Host "Restart failed: $($_.Exception.Message)"
  exit 1
}

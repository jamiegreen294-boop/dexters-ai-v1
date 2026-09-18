$ErrorActionPreference = "SilentlyContinue"
$health = Invoke-RestMethod -Uri "http://127.0.0.1:8787/health" -TimeoutSec 5
if ($health.status -eq "ready") {
  Write-Host "Dexter AI is healthy."
  exit 0
}
Write-Host "Dexter AI is not healthy. Restarting scheduled task..."
Stop-ScheduledTask -TaskName "Dexter AI Home Host"
Start-ScheduledTask -TaskName "Dexter AI Home Host"

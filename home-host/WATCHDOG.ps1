$ErrorActionPreference = "SilentlyContinue"
$taskName = "Dexter AI Home Host"
$healthy = $false
try {
  $health = Invoke-RestMethod -Uri "http://127.0.0.1:8787/health" -TimeoutSec 5
  $healthy = ($health.status -eq "ready")
} catch {
  $healthy = $false
}

if ($healthy) {
  Write-Host "Dexter AI is healthy."
  exit 0
}

Write-Host "Dexter AI is not healthy. Forcing Home Host restart..."
try { Stop-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue } catch {}
Start-Sleep -Seconds 2
try {
  Start-ScheduledTask -TaskName $taskName
  Write-Host "Restart requested."
} catch {
  Write-Host "Restart failed: $($_.Exception.Message)"
  exit 1
}

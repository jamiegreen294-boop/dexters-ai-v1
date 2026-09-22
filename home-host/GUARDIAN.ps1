$ErrorActionPreference = "SilentlyContinue"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$bat = Join-Path $root "START-DEXTER.bat"
$server = Join-Path $root "server.mjs"
$log = Join-Path $root "guardian.log"
$base = "https://raw.githubusercontent.com/jamiegreen294-boop/dexters-ai-v1/build/real-dexter-ai/home-host/"

function Log($m) {
  try { Add-Content -Path $log -Value ("{0:o} {1}" -f (Get-Date),$m) -Encoding UTF8 } catch {}
}
function Healthy {
  try {
    $h = Invoke-RestMethod -Uri "http://127.0.0.1:8787/health" -TimeoutSec 4
    return ($h.status -eq "ready")
  } catch { return $false }
}
function Refresh-Core {
  foreach($f in @("server.mjs","START-DEXTER.bat","WATCHDOG.ps1")) {
    try {
      $tmp = Join-Path $env:TEMP ("dexter-"+[guid]::NewGuid().ToString("N")+"-"+$f)
      Invoke-WebRequest -UseBasicParsing ($base+$f+"?t="+[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()) -OutFile $tmp -TimeoutSec 20
      if($f -eq "server.mjs") {
        & node --check $tmp *> $null
        if($LASTEXITCODE -ne 0) { Remove-Item $tmp -Force -ErrorAction SilentlyContinue; continue }
      }
      Move-Item -Force $tmp (Join-Path $root $f)
    } catch { Log ("refresh failed "+$f+": "+$_.Exception.Message) }
  }
}
if(Healthy){ exit 0 }

Log "health check failed; recovering"
Refresh-Core

# Kill only stale Dexter Home Host node processes, never unrelated Node apps.
try {
  Get-CimInstance Win32_Process -Filter "Name='node.exe'" | Where-Object {
    $_.CommandLine -and $_.CommandLine -match [regex]::Escape($server)
  } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
} catch {}

Start-Sleep -Seconds 2
try {
  $task = Get-ScheduledTask -TaskName "Dexter AI Home Host" -ErrorAction SilentlyContinue
  if($task) {
    Start-ScheduledTask -TaskName "Dexter AI Home Host"
  } elseif(Test-Path $bat) {
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c `"$bat`"" -WorkingDirectory $root -WindowStyle Hidden
  }
} catch {
  try { Start-Process -FilePath "cmd.exe" -ArgumentList "/c `"$bat`"" -WorkingDirectory $root -WindowStyle Hidden } catch {}
}
Start-Sleep -Seconds 10
if(Healthy){ Log "recovered"; exit 0 }
Log "recovery attempt did not become healthy"
exit 1

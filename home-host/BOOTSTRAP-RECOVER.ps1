$ErrorActionPreference = "Stop"

$base = "https://raw.githubusercontent.com/jamiegreen294-boop/dexters-ai-v1/build/real-dexter-ai/home-host/"
$root = Join-Path $env:USERPROFILE "DexterAI\dexters-ai-v1-build-real-dexter-ai\home-host"
New-Item -ItemType Directory -Force -Path $root | Out-Null

$files = @("server.mjs","START-DEXTER.bat","WATCHDOG.ps1","REGISTER-AUTOSTART.ps1")
foreach($f in $files){
  $u = $base + $f + "?t=" + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
  Invoke-WebRequest -UseBasicParsing $u -OutFile (Join-Path $root $f)
}

Push-Location $root
try {
  & node --check ".\server.mjs"
  if($LASTEXITCODE -ne 0){ throw "Downloaded server.mjs failed validation." }

  powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\REGISTER-AUTOSTART.ps1"
  Start-Sleep -Seconds 10

  try {
    $h = Invoke-RestMethod -Uri "http://127.0.0.1:8787/health" -TimeoutSec 5
    if($h.status -ne "ready"){ throw "Health endpoint not ready." }
    Write-Host "DEXTER_OK"
  } catch {
    Start-ScheduledTask -TaskName "Dexter AI Home Host" -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 8
    $h = Invoke-RestMethod -Uri "http://127.0.0.1:8787/health" -TimeoutSec 5
    if($h.status -ne "ready"){ throw "Dexter AI did not become healthy after restart." }
    Write-Host "DEXTER_OK"
  }
}
finally { Pop-Location }

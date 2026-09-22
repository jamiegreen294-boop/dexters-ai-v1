@echo off
setlocal
cd /d %~dp0
echo Repairing Dexter AI Home Host...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$base='https://raw.githubusercontent.com/jamiegreen294-boop/dexters-ai-v1/build/real-dexter-ai/home-host/'; foreach($f in 'START-DEXTER.bat','WATCHDOG.ps1','REGISTER-AUTOSTART.ps1','server.mjs'){Invoke-WebRequest -UseBasicParsing ($base+$f+'?t='+[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()) -OutFile (Join-Path $PWD $f)}"
if errorlevel 1 (
  echo Repair download failed.
  pause
  exit /b 1
)
node --check server.mjs
if errorlevel 1 (
  echo server.mjs failed validation.
  pause
  exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -File .\REGISTER-AUTOSTART.ps1
echo Dexter AI recovery installed.
timeout /t 3 /nobreak >nul

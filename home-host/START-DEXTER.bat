@echo off
setlocal EnableExtensions
cd /d %~dp0

echo Checking for Dexter Home Host updates...
for /f %%i in ('powershell -NoProfile -Command "[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()"') do set DEXTER_UPDATE=%%i
curl.exe -L --fail --silent --show-error "https://raw.githubusercontent.com/jamiegreen294-boop/dexters-ai-v1/build/real-dexter-ai/home-host/server.mjs?dexter_update=%DEXTER_UPDATE%" -o "%TEMP%\dexter-server.mjs"
if not errorlevel 1 (
  copy /Y "%TEMP%\dexter-server.mjs" "%~dp0server.mjs" >nul
  echo Dexter Home Host updated.
) else (
  echo Update check failed - starting installed version.
)

if "%DEXTER_BROWSER_WORKER_TOKEN%"=="" (
  echo Dexter browser token is not loaded in this terminal.
  echo Close this window and reopen Command Prompt after installation.
  pause
  exit /b 1
)

npm start

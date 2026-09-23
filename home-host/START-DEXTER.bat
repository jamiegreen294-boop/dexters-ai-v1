@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d %~dp0

set "DEXTER_SERVER=%~dp0server.mjs"
set "DEXTER_BACKUP=%~dp0server.mjs.last-good"
set "DEXTER_DOWNLOAD=%TEMP%\dexter-server-%RANDOM%.mjs"

:restart
echo Checking for Dexter Home Host updates...
for /f %%i in ('powershell -NoProfile -Command "[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()"') do set DEXTER_UPDATE=%%i
curl.exe -L --fail --silent --show-error "https://raw.githubusercontent.com/jamiegreen294-boop/dexters-ai-v1/build/real-dexter-ai/home-host/server.mjs?dexter_update=!DEXTER_UPDATE!" -o "!DEXTER_DOWNLOAD!"

if not errorlevel 1 (
  node --check "!DEXTER_DOWNLOAD!" >nul 2>&1
  if not errorlevel 1 (
    if exist "!DEXTER_SERVER!" copy /Y "!DEXTER_SERVER!" "!DEXTER_BACKUP!" >nul
    copy /Y "!DEXTER_DOWNLOAD!" "!DEXTER_SERVER!" >nul
    echo Dexter Home Host update validated and installed.
  ) else (
    echo Downloaded Dexter update failed syntax validation. Keeping last working version.
  )
) else (
  echo Update check failed - keeping installed version.
)
del /Q "!DEXTER_DOWNLOAD!" >nul 2>&1

node --check "!DEXTER_SERVER!" >nul 2>&1
if errorlevel 1 (
  echo Installed Dexter server is invalid.
  if exist "!DEXTER_BACKUP!" (
    echo Restoring last known good server...
    copy /Y "!DEXTER_BACKUP!" "!DEXTER_SERVER!" >nul
  )
)

if "%DEXTER_BROWSER_WORKER_TOKEN%"=="" (
  for /f "usebackq delims=" %%T in (`powershell -NoProfile -Command "[Environment]::GetEnvironmentVariable('DEXTER_BROWSER_WORKER_TOKEN','User')"`) do set "DEXTER_BROWSER_WORKER_TOKEN=%%T"
)
if "%DEXTER_AGENT_TOKEN%"=="" (
  for /f "usebackq delims=" %%T in (`powershell -NoProfile -Command "[Environment]::GetEnvironmentVariable('DEXTER_AGENT_TOKEN','User')"`) do set "DEXTER_AGENT_TOKEN=%%T"
)

if "%DEXTER_BROWSER_WORKER_TOKEN%"=="" (
  echo Dexter browser token is missing. Waiting 30 seconds before retry...
  timeout /t 30 /nobreak >nul
  goto restart
)

powershell -NoProfile -Command "if(-not (Get-NetTCPConnection -LocalPort 11434 -State Listen -ErrorAction SilentlyContinue)){ $o=Join-Path $env:LOCALAPPDATA 'Programs\Ollama\ollama.exe'; if(Test-Path $o){ Start-Process -FilePath $o -ArgumentList 'serve' -WindowStyle Hidden } }" >nul 2>&1
echo Starting Ollama local AI if needed...
timeout /t 2 /nobreak >nul

echo Starting Dexter AI Home Host...
node "!DEXTER_SERVER!"
echo Dexter Home Host stopped with exit code %ERRORLEVEL%. Restarting in 3 seconds...
timeout /t 3 /nobreak >nul
goto restart

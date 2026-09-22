@echo off
setlocal EnableExtensions
set "ROOT=%USERPROFILE%\DexterAI\dexters-ai-v1-build-real-dexter-ai\home-host"
if not exist "%ROOT%" mkdir "%ROOT%"
cd /d "%ROOT%"

echo Repairing Dexter AI without PowerShell...
for %%F in (server.mjs START-DEXTER.bat GUARDIAN.cjs REGISTER-AUTOSTART.cmd) do (
  curl.exe -L --fail --silent --show-error "https://raw.githubusercontent.com/jamiegreen294-boop/dexters-ai-v1/build/real-dexter-ai/home-host/%%F?fix=%RANDOM%" -o "%%F.new"
  if errorlevel 1 (
    echo Failed to download %%F
    exit /b 1
  )
  move /Y "%%F.new" "%%F" >nul
)

node --check server.mjs
if errorlevel 1 (
  echo server.mjs failed validation.
  exit /b 1
)

call REGISTER-AUTOSTART.cmd
echo Dexter AI recovery complete.
exit /b 0

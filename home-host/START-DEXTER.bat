@echo off
cd /d %~dp0
if "%DEXTER_BROWSER_WORKER_TOKEN%"=="" (
  echo Dexter browser token is not loaded in this terminal.
  echo Close this window and reopen Command Prompt after running install.ps1.
  pause
  exit /b 1
)
npm start

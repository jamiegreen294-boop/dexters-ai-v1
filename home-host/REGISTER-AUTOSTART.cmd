@echo off
setlocal EnableExtensions
cd /d "%~dp0"
set "ROOT=%~dp0"
set "START=%ROOT%START-DEXTER.bat"
set "GUARD=%ROOT%GUARDIAN.cjs"

echo Registering Dexter AI startup...
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v DexterAIHomeHost /t REG_SZ /d "cmd.exe /c start \"\" /min \"%START%\"" /f >nul 2>&1

schtasks /Create /TN "Dexter AI Home Host" /TR "cmd.exe /c \"%START%\"" /SC ONLOGON /F >nul 2>&1
schtasks /Create /TN "Dexter AI Guardian" /TR "node \"%GUARD%\"" /SC MINUTE /MO 1 /F >nul 2>&1

schtasks /Run /TN "Dexter AI Home Host" >nul 2>&1
node "%GUARD%" >nul 2>&1

echo Dexter AI startup and Node guardian registered.
exit /b 0

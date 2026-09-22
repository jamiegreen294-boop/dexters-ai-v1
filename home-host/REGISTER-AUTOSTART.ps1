$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$bat = Join-Path $root "START-DEXTER.bat"
$watchdog = Join-Path $root "WATCHDOG.ps1"

$mainAction = New-ScheduledTaskAction -Execute "cmd.exe" -Argument "/c `"$bat`""
$startupTrigger = New-ScheduledTaskTrigger -AtStartup
$logonTrigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$mainSettings = New-ScheduledTaskSettingsSet -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1) -StartWhenAvailable -MultipleInstances IgnoreNew
$principal = New-ScheduledTaskPrincipal -UserId $env:USERNAME -LogonType Interactive -RunLevel Limited

Register-ScheduledTask -TaskName "Dexter AI Home Host" -Action $mainAction -Trigger @($startupTrigger,$logonTrigger) -Settings $mainSettings -Principal $principal -Force

# Independent watchdog: this is what lets Dexter recover when the Node process
# itself is dead and therefore cannot self-heal.
$watchAction = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$watchdog`""
$watchTrigger = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) -RepetitionInterval (New-TimeSpan -Minutes 1) -RepetitionDuration ([TimeSpan]::MaxValue)
$watchSettings = New-ScheduledTaskSettingsSet -StartWhenAvailable -MultipleInstances IgnoreNew
Register-ScheduledTask -TaskName "Dexter AI Home Host Watchdog" -Action $watchAction -Trigger $watchTrigger -Settings $watchSettings -Principal $principal -Force

# Start it immediately as part of registration.
Start-ScheduledTask -TaskName "Dexter AI Home Host"
Write-Host "Dexter AI Home Host and 1-minute self-healing watchdog are registered and started."

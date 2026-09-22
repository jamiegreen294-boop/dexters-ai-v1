$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$bat = Join-Path $root "START-DEXTER.bat"
$watchdog = Join-Path $root "WATCHDOG.ps1"
$guardian = Join-Path $root "GUARDIAN.ps1"

$mainAction = New-ScheduledTaskAction -Execute "cmd.exe" -Argument "/c `"$bat`""
$startupTrigger = New-ScheduledTaskTrigger -AtStartup
$logonTrigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$mainSettings = New-ScheduledTaskSettingsSet -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1) -StartWhenAvailable -MultipleInstances IgnoreNew
$mainPrincipal = New-ScheduledTaskPrincipal -UserId $env:USERNAME -LogonType Interactive -RunLevel Limited
Register-ScheduledTask -TaskName "Dexter AI Home Host" -Action $mainAction -Trigger @($startupTrigger,$logonTrigger) -Settings $mainSettings -Principal $mainPrincipal -Force

# Independent guardian. Prefer SYSTEM so it survives logoff and Home Host crashes.
$guardianAction = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$guardian`""
$guardianTriggers = @(
  (New-ScheduledTaskTrigger -AtStartup),
  (New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME),
  (New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) -RepetitionInterval (New-TimeSpan -Minutes 1) -RepetitionDuration ([TimeSpan]::MaxValue))
)
$guardianSettings = New-ScheduledTaskSettingsSet -StartWhenAvailable -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Minutes 2)
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if($isAdmin) {
  $guardianPrincipal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -LogonType ServiceAccount -RunLevel Highest
} else {
  $guardianPrincipal = New-ScheduledTaskPrincipal -UserId $env:USERNAME -LogonType Interactive -RunLevel Limited
}
Register-ScheduledTask -TaskName "Dexter AI Guardian" -Action $guardianAction -Trigger $guardianTriggers -Settings $guardianSettings -Principal $guardianPrincipal -Force

# Compatibility watchdog task retained too.
$watchAction = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$watchdog`""
$watchSettings = New-ScheduledTaskSettingsSet -StartWhenAvailable -MultipleInstances IgnoreNew
Register-ScheduledTask -TaskName "Dexter AI Home Host Watchdog" -Action $watchAction -Trigger $guardianTriggers -Settings $watchSettings -Principal $mainPrincipal -Force

# Fallback at user logon even if Task Scheduler registration is damaged.
$runValue = 'cmd.exe /c start "" /min "' + $bat + '"'
New-Item -Path "HKCU:\Software\Microsoft\Windows\CurrentVersion\Run" -Force | Out-Null
New-ItemProperty -Path "HKCU:\Software\Microsoft\Windows\CurrentVersion\Run" -Name "DexterAIHomeHost" -Value $runValue -PropertyType String -Force | Out-Null

Start-ScheduledTask -TaskName "Dexter AI Home Host"
Start-ScheduledTask -TaskName "Dexter AI Guardian"
Write-Host "Dexter AI Home Host, Guardian, watchdog and logon fallback are registered and started."

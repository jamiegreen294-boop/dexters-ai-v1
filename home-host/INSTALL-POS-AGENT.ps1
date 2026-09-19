$ErrorActionPreference = "Stop"
$root = Join-Path $env:LOCALAPPDATA "DexterPOSAgent"
New-Item -ItemType Directory -Force -Path $root | Out-Null
Set-Location $root

function Download-DexterFile($name) {
  $base = "https://raw.githubusercontent.com/jamiegreen294-boop/dexters-ai-v1/build/real-dexter-ai/home-host/"
  Invoke-WebRequest -UseBasicParsing -Uri ($base + $name + "?t=" + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()) -OutFile (Join-Path $root $name)
}

Write-Host "Installing Dexter POS PC Agent..."
Download-DexterFile "server.mjs"
Download-DexterFile "package.json"
Download-DexterFile "START-DEXTER.bat"

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
  if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    throw "Node.js is required and winget is not available."
  }
  winget install --id OpenJS.NodeJS.LTS -e --accept-package-agreements --accept-source-agreements
  $env:Path = [Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [Environment]::GetEnvironmentVariable("Path","User")
}

npm install --omit=dev

$browserToken = [Environment]::GetEnvironmentVariable("DEXTER_BROWSER_WORKER_TOKEN","User")
if ([string]::IsNullOrWhiteSpace($browserToken)) {
  $bytes = New-Object byte[] 32
  [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
  $browserToken = [Convert]::ToBase64String($bytes)
  [Environment]::SetEnvironmentVariable("DEXTER_BROWSER_WORKER_TOKEN",$browserToken,"User")
}
$env:DEXTER_BROWSER_WORKER_TOKEN = $browserToken
[Environment]::SetEnvironmentVariable("DEXTER_LIVE_ACTIONS","false","User")
$env:DEXTER_LIVE_ACTIONS = "false"

$endpoint = "https://eikruaxxzzxmfjvsmwwo.supabase.co/functions/v1/dexter-home-agent"
$code = Read-Host "Enter the Dexter POS pairing code"
if ([string]::IsNullOrWhiteSpace($code)) { throw "Pairing code is required." }

$body = @{
  action = "pair_claim"
  code = $code.Trim()
  name = "Dexter POS PC - $env:COMPUTERNAME"
} | ConvertTo-Json
$response = Invoke-RestMethod -Method Post -Uri $endpoint -ContentType "application/json" -Body $body
if (-not $response.paired -or -not $response.agent_token) { throw "Pairing failed." }

[Environment]::SetEnvironmentVariable("DEXTER_AGENT_ENDPOINT",$endpoint,"User")
[Environment]::SetEnvironmentVariable("DEXTER_AGENT_TOKEN",$response.agent_token,"User")
$env:DEXTER_AGENT_ENDPOINT = $endpoint
$env:DEXTER_AGENT_TOKEN = $response.agent_token

$taskName = "Dexter POS Hardware Agent"
$bat = Join-Path $root "START-DEXTER.bat"
$action = New-ScheduledTaskAction -Execute "cmd.exe" -Argument "/c `"$bat`""
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$settings = New-ScheduledTaskSettingsSet -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1) -StartWhenAvailable
$principal = New-ScheduledTaskPrincipal -UserId $env:USERNAME -LogonType Interactive -RunLevel Limited
Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings -Principal $principal -Force | Out-Null

Start-Process -FilePath "cmd.exe" -ArgumentList "/c `"$bat`"" -WorkingDirectory $root -WindowStyle Minimized

Write-Host ""
Write-Host "Dexter POS PC Agent installed and paired."
Write-Host "Hardware Doctor will now be available to Dexter AI."

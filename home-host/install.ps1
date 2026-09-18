$ErrorActionPreference = "Stop"
Write-Host "Installing Dexter AI home host..."
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
  Write-Host "Node.js is required. Install Node.js LTS, then run this script again."
  exit 1
}
npm install
npm run install-browser
if (-not $env:DEXTER_BROWSER_WORKER_TOKEN) {
  $bytes = New-Object byte[] 32
  [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
  $token = [Convert]::ToBase64String($bytes)
  [Environment]::SetEnvironmentVariable("DEXTER_BROWSER_WORKER_TOKEN",$token,"User")
  Write-Host "Created a Dexter browser token for this Windows user."
}
Write-Host "Install complete. Close and reopen PowerShell, then run START-DEXTER.bat"

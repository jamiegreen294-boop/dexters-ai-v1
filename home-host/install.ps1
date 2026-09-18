$ErrorActionPreference = "Stop"
Write-Host "Installing Dexter AI Home Host..."

function Ensure-WingetPackage($commandName, $packageId, $label) {
  if (Get-Command $commandName -ErrorAction SilentlyContinue) {
    Write-Host "$label already installed."
    return
  }
  if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    throw "$label is required and winget is not available. Install $label, then run this installer again."
  }
  Write-Host "Installing $label..."
  winget install --id $packageId -e --accept-package-agreements --accept-source-agreements
  $env:Path = [Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [Environment]::GetEnvironmentVariable("Path","User")
}

Ensure-WingetPackage "node" "OpenJS.NodeJS.LTS" "Node.js LTS"
Ensure-WingetPackage "git" "Git.Git" "Git"
Ensure-WingetPackage "gh" "GitHub.cli" "GitHub CLI"

npm install
npm run install-browser

if (-not [Environment]::GetEnvironmentVariable("DEXTER_BROWSER_WORKER_TOKEN","User")) {
  $bytes = New-Object byte[] 32
  [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
  $token = [Convert]::ToBase64String($bytes)
  [Environment]::SetEnvironmentVariable("DEXTER_BROWSER_WORKER_TOKEN",$token,"User")
  Write-Host "Created Dexter's private home-worker token."
}

[Environment]::SetEnvironmentVariable("DEXTER_LIVE_ACTIONS","false","User")

if (-not (Get-Command ollama -ErrorAction SilentlyContinue)) {
  Write-Host "Installing free local AI runtime (Ollama)..."
  try {
    irm https://ollama.com/install.ps1 | iex
  } catch {
    Write-Host "Automatic Ollama install failed. Dexter can still run with the configured cloud AI provider until Ollama is installed."
  }
  $env:Path = [Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [Environment]::GetEnvironmentVariable("Path","User")
}

if (Get-Command ollama -ErrorAction SilentlyContinue) {
  Write-Host "Downloading Dexter's local model..."
  ollama pull qwen3:4b
  [Environment]::SetEnvironmentVariable("DEXTER_LOCAL_MODEL","qwen3:4b","User")
}

New-Item -ItemType Directory -Force -Path ".\workspace" | Out-Null
New-Item -ItemType Directory -Force -Path ".\jobs" | Out-Null
New-Item -ItemType Directory -Force -Path ".\browser-profile" | Out-Null

Write-Host ""
Write-Host "Dexter AI Home Host install complete."
Write-Host "Live actions are OFF."
Write-Host "For GitHub write access later, run: gh auth login"
Write-Host "For Vercel write access later, run: npx vercel login"
Write-Host "Reopen PowerShell/Command Prompt, then run START-DEXTER.bat"

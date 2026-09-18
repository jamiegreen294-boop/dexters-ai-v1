$ErrorActionPreference = "Stop"
Write-Host "Installing Dexter AI Home Host v2..."

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
  Write-Host "Node.js LTS is required. Install it first, then run this script again."
  exit 1
}

npm install
npm run install-browser

if (-not $env:DEXTER_BROWSER_WORKER_TOKEN) {
  $bytes = New-Object byte[] 32
  [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
  $token = [Convert]::ToBase64String($bytes)
  [Environment]::SetEnvironmentVariable("DEXTER_BROWSER_WORKER_TOKEN",$token,"User")
  Write-Host "Created Dexter's local worker token."
}

if (-not (Get-Command ollama -ErrorAction SilentlyContinue)) {
  Write-Host "Installing free local AI runtime (Ollama)..."
  try {
    irm https://ollama.com/install.ps1 | iex
  } catch {
    Write-Host "Ollama automatic install failed. Dexter can still start, but local AI will remain unavailable until Ollama is installed."
  }
}

if (Get-Command ollama -ErrorAction SilentlyContinue) {
  Write-Host "Downloading Dexter's default local model qwen3:4b..."
  ollama pull qwen3:4b
  [Environment]::SetEnvironmentVariable("DEXTER_LOCAL_MODEL","qwen3:4b","User")
}

Write-Host ""
Write-Host "Dexter AI Home Host install complete."
Write-Host "Reopen PowerShell/Command Prompt, then run START-DEXTER.bat"

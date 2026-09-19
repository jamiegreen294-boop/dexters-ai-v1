$ErrorActionPreference = "Stop"
Write-Host "Dexter AI free remote access setup"
if (-not (Get-Command cloudflared -ErrorAction SilentlyContinue)) {
  if (Get-Command winget -ErrorAction SilentlyContinue) {
    winget install --id Cloudflare.cloudflared -e --accept-package-agreements --accept-source-agreements
  } else {
    Write-Host "Install cloudflared from Cloudflare, then run this script again."
    exit 1
  }
}
Write-Host ""
Write-Host "Next, log in to Cloudflare once:"
Write-Host "  cloudflared tunnel login"
Write-Host ""
Write-Host "Then create a named tunnel:"
Write-Host "  cloudflared tunnel create dexter-ai"
Write-Host ""
Write-Host "Route a hostname you own, for example ai.dextersspot.co.uk:"
Write-Host "  cloudflared tunnel route dns dexter-ai ai.dextersspot.co.uk"
Write-Host ""
Write-Host "Create %USERPROFILE%\.cloudflared\config.yml with your tunnel ID and:"
Write-Host "  ingress:"
Write-Host "    - hostname: ai.dextersspot.co.uk"
Write-Host "      service: http://127.0.0.1:8787"
Write-Host "    - service: http_status:404"
Write-Host ""
Write-Host "Then install the tunnel as a Windows service:"
Write-Host "  cloudflared service install"

$ErrorActionPreference = "Stop"
$endpoint = "https://eikruaxxzzxmfjvsmwwo.supabase.co/functions/v1/dexter-home-agent"
Write-Host "Pair Dexter Home PC with the Dexter AI cloud workspace"
$code = Read-Host "Enter the pairing code shown in Dexter AI > System"
if ([string]::IsNullOrWhiteSpace($code)) { throw "Pairing code is required." }

$body = @{
  action = "pair_claim"
  code = $code.Trim()
  name = "Dexter Home PC - $env:COMPUTERNAME"
} | ConvertTo-Json

$response = Invoke-RestMethod -Method Post -Uri $endpoint -ContentType "application/json" -Body $body
if (-not $response.paired -or -not $response.agent_token) { throw "Pairing failed." }

[Environment]::SetEnvironmentVariable("DEXTER_AGENT_ENDPOINT",$endpoint,"User")
[Environment]::SetEnvironmentVariable("DEXTER_AGENT_TOKEN",$response.agent_token,"User")

Write-Host ""
Write-Host "Dexter Home PC paired successfully."
Write-Host "Close and restart Dexter (START-DEXTER.bat) so it loads the pairing token."

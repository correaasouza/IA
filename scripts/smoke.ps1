param(
  [string]$KeycloakBaseUrl = "http://host.docker.internal:8081",
  [string]$Realm = "ia",
  [string]$ClientId = "frontend",
  [string]$BackendBaseUrl = "http://localhost:8082",
  [string]$Username = "master",
  [string]$Password = "master123"
)

$ErrorActionPreference = "Stop"

function Decode-JwtPayload([string]$Jwt) {
  $parts = $Jwt.Split('.')
  if ($parts.Length -lt 2) { throw "Invalid JWT" }
  $payload = $parts[1].Replace('-', '+').Replace('_', '/')
  switch ($payload.Length % 4) {
    0 { }
    2 { $payload += "==" }
    3 { $payload += "=" }
    default { throw "Invalid base64url length" }
  }
  $bytes = [Convert]::FromBase64String($payload)
  $json = [Text.Encoding]::UTF8.GetString($bytes)
  return $json | ConvertFrom-Json
}

function Get-AccessToken([string]$User, [string]$Pass) {
  $tokenUrl = "$KeycloakBaseUrl/realms/$Realm/protocol/openid-connect/token"
  $body = @{
    grant_type = "password"
    client_id  = $ClientId
    username   = $User
    password   = $Pass
  }
  $resp = Invoke-RestMethod -Method Post -Uri $tokenUrl -ContentType "application/x-www-form-urlencoded" -Body $body
  return $resp.access_token
}

function Invoke-Api([string]$Method, [string]$Url, [hashtable]$Headers = @{}, $Body = $null) {
  if ($null -eq $Body) {
    return Invoke-WebRequest -Method $Method -Uri $Url -Headers $Headers -UseBasicParsing -SkipHttpErrorCheck
  }
  return Invoke-WebRequest -Method $Method -Uri $Url -Headers $Headers -Body $Body -ContentType "application/json" -UseBasicParsing -SkipHttpErrorCheck
}

Write-Host "Health (no auth): $BackendBaseUrl/actuator/health"
$health = Invoke-Api -Method Get -Url "$BackendBaseUrl/actuator/health"
Write-Host "  status=$($health.StatusCode)"

Write-Host "Token: $Username (Keycloak $KeycloakBaseUrl realm=$Realm client=$ClientId)"
$token = Get-AccessToken -User $Username -Pass $Password
$payload = Decode-JwtPayload -Jwt $token
Write-Host "  iss=$($payload.iss)"
Write-Host "  sub=$($payload.sub)"

$authHeaders = @{
  Authorization = "Bearer $token"
}

Write-Host "GET /api/me (auth)"
$me = Invoke-Api -Method Get -Url "$BackendBaseUrl/api/me" -Headers $authHeaders
Write-Host "  status=$($me.StatusCode)"
Write-Host "  body=$($me.Content)"

Write-Host "GET /api/locatarios/allowed (auth)"
$allowed = Invoke-Api -Method Get -Url "$BackendBaseUrl/api/locatarios/allowed" -Headers $authHeaders
Write-Host "  status=$($allowed.StatusCode)"
Write-Host "  body=$($allowed.Content)"

Write-Host "Tenant enforcement check (no X-Tenant-Id on a non-open endpoint)"
$enforce = Invoke-Api -Method Get -Url "$BackendBaseUrl/api/usuarios?page=0&size=1" -Headers $authHeaders
Write-Host "  status=$($enforce.StatusCode)"
if ($enforce.Content) { Write-Host "  body=$($enforce.Content)" }

Write-Host "Done."


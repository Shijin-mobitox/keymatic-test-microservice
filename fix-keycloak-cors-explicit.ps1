# Script to add explicit tenant subdomain origins to Keycloak client
param(
    [string]$KeycloakUrl = "http://localhost:8085",
    [string]$Realm = "kymatic",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin",
    [string]$ClientId = "react-client"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Adding Explicit Tenant Subdomain Origins" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Get admin token
Write-Host "Step 1: Getting admin access token..." -ForegroundColor Yellow
$tokenUrl = "$KeycloakUrl/realms/master/protocol/openid-connect/token"
$tokenBody = @{
    grant_type = "password"
    client_id = "admin-cli"
    username = $AdminUser
    password = $AdminPassword
}

try {
    $tokenResponse = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $tokenBody -ContentType "application/x-www-form-urlencoded"
    $adminToken = $tokenResponse.access_token
    Write-Host "[OK] Admin token obtained" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to get admin token: $_" -ForegroundColor Red
    exit 1
}

# Step 2: Get client
Write-Host ""
Write-Host "Step 2: Getting client configuration..." -ForegroundColor Yellow
$headers = @{
    "Authorization" = "Bearer $adminToken"
    "Content-Type" = "application/json"
}

$getClientUrl = "$KeycloakUrl/admin/realms/$Realm/clients"
$clients = Invoke-RestMethod -Uri $getClientUrl -Method Get -Headers $headers
$client = $clients | Where-Object { $_.clientId -eq $ClientId }

if (-not $client) {
    Write-Host "[ERROR] Client '$ClientId' not found" -ForegroundColor Red
    exit 1
}

# Step 3: Get full client config
Write-Host "[OK] Client found, getting full configuration..." -ForegroundColor Green
$getClientDetailUrl = "$KeycloakUrl/admin/realms/$Realm/clients/$($client.id)"
$fullClient = Invoke-RestMethod -Uri $getClientDetailUrl -Method Get -Headers $headers

# Step 4: Update with explicit origins
Write-Host ""
Write-Host "Step 3: Updating with explicit origins..." -ForegroundColor Yellow

# Add explicit origins for localhost subdomains
$webOrigins = @(
    "http://localhost:5173",
    "http://localhost:3000",
    "+"  # Keep wildcard as fallback
)

$redirectUris = @(
    "http://localhost:5173/*",
    "http://localhost:3000/*",
    "http://localhost:5173/",
    "http://localhost:3000/",
    "*"  # Wildcard for tenant subdomains
)

$fullClient.webOrigins = $webOrigins
$fullClient.redirectUris = $redirectUris
$fullClient.publicClient = $true
$fullClient.enabled = $true
$fullClient.standardFlowEnabled = $true

$updateBody = $fullClient | ConvertTo-Json -Depth 10

try {
    Invoke-RestMethod -Uri $getClientDetailUrl -Method Put -Headers $headers -Body $updateBody -ContentType "application/json" | Out-Null
    Write-Host "[OK] Client updated with explicit origins!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Web Origins:" -ForegroundColor Cyan
    $webOrigins | ForEach-Object { Write-Host "  - $_" -ForegroundColor White }
    Write-Host ""
    Write-Host "Redirect URIs:" -ForegroundColor Cyan
    $redirectUris | ForEach-Object { Write-Host "  - $_" -ForegroundColor White }
} catch {
    Write-Host "[ERROR] Failed to update: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Configuration Complete!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Note: You may need to restart Keycloak for changes to take effect:" -ForegroundColor Yellow
Write-Host "  docker-compose restart keycloak" -ForegroundColor White


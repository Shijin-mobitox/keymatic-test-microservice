# Script to fix Keycloak client configuration via Admin API
# This script updates the react-client configuration to have proper redirect URIs

param(
    [string]$KeycloakUrl = "http://localhost:8085",
    [string]$Realm = "kymatic",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin",
    [string]$ClientId = "react-client"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Keycloak Client Configuration Fix" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Get admin token
Write-Host "Step 1: Getting admin access token..." -ForegroundColor Yellow
$tokenUrl = "$KeycloakUrl/realms/master/protocol/openid-connect/token"
$tokenBody = "grant_type=password&client_id=admin-cli&username=$AdminUser&password=$AdminPassword"

try {
    $tokenResponse = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $tokenBody -ContentType "application/x-www-form-urlencoded"
    $adminToken = $tokenResponse.access_token
    Write-Host "[OK] Admin token obtained" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to get admin token: $_" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
    exit 1
}

# Step 2: Get current client configuration
Write-Host ""
Write-Host "Step 2: Getting current client configuration..." -ForegroundColor Yellow
$getClientUrl = "$KeycloakUrl/admin/realms/$Realm/clients"
$headers = @{
    "Authorization" = "Bearer $adminToken"
    "Content-Type" = "application/json"
}

try {
    $clients = Invoke-RestMethod -Uri $getClientUrl -Method Get -Headers $headers
    $client = $clients | Where-Object { $_.clientId -eq $ClientId }
    
    if (-not $client) {
        Write-Host "[ERROR] Client '$ClientId' not found in realm '$Realm'" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[OK] Client found: $($client.id)" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to get client: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Update client configuration
Write-Host ""
Write-Host "Step 3: Updating client configuration..." -ForegroundColor Yellow

$updateUrl = "$KeycloakUrl/admin/realms/$Realm/clients/$($client.id)"
$updateBody = @{
    clientId = $ClientId
    enabled = $true
    publicClient = $true
    redirectUris = @(
        "*"
    )
    webOrigins = @(
        "+"
    )
    standardFlowEnabled = $true
    directAccessGrantsEnabled = $false
    implicitFlowEnabled = $false
} | ConvertTo-Json -Depth 10

try {
    Invoke-RestMethod -Uri $updateUrl -Method Put -Headers $headers -Body $updateBody -ContentType "application/json"
    Write-Host "[OK] Client configuration updated successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Updated redirect URIs:" -ForegroundColor Cyan
    Write-Host "  - * (all URIs - allows tenant subdomains)" -ForegroundColor White
    Write-Host ""
    Write-Host "Updated web origins:" -ForegroundColor Cyan
    Write-Host "  - + (all origins - allows tenant subdomains)" -ForegroundColor White
} catch {
    Write-Host "[ERROR] Failed to update client: $_" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Configuration complete!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "You can now try accessing the frontend again." -ForegroundColor Yellow


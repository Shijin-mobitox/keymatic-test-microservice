# Script to enable password grant (Direct Access Grants) for react-client in Keycloak
# This allows the client to use username/password login without Keycloak's UI

param(
    [string]$KeycloakUrl = "http://localhost:8085",
    [string]$Realm = "kymatic",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin",
    [string]$ClientId = "react-client"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Enable Password Grant for React Client" -ForegroundColor Cyan
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
    
    Write-Host "[OK] Found client: $($client.id)" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to get client: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Update client to enable Direct Access Grants
Write-Host ""
Write-Host "Step 3: Enabling Direct Access Grants (password grant)..." -ForegroundColor Yellow
$updateClientUrl = "$KeycloakUrl/admin/realms/$Realm/clients/$($client.id)"
$clientUpdate = @{
    directAccessGrantsEnabled = $true
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri $updateClientUrl -Method Put -Body $clientUpdate -Headers $headers | Out-Null
    Write-Host "[OK] Direct Access Grants enabled successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "The react-client can now use password grant type for authentication." -ForegroundColor Green
    Write-Host "Users can login with username/password directly." -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to update client: $_" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Configuration Complete!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan


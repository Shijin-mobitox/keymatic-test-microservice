# Script to update Keycloak client redirect URIs to allow all URIs
param(
    [string]$KeycloakUrl = "http://localhost:8085",
    [string]$Realm = "kymatic",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin",
    [string]$ClientId = "react-client"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Updating Keycloak Client Redirect URIs" -ForegroundColor Cyan
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
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
    exit 1
}

# Step 2: Get current client configuration
Write-Host ""
Write-Host "Step 2: Getting client configuration..." -ForegroundColor Yellow
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
    Write-Host "Current redirect URIs: $($client.redirectUris -join ', ')" -ForegroundColor Gray
} catch {
    Write-Host "[ERROR] Failed to get client: $_" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
    exit 1
}

# Step 3: Update client configuration to allow all redirect URIs
Write-Host ""
Write-Host "Step 3: Updating client to allow all redirect URIs..." -ForegroundColor Yellow

$updateUrl = "$KeycloakUrl/admin/realms/$Realm/clients/$($client.id)"
$updateBody = @{
    clientId = $ClientId
    enabled = $true
    publicClient = $true
    redirectUris = @("*")
    webOrigins = @("+")
    standardFlowEnabled = $true
    directAccessGrantsEnabled = $false
    implicitFlowEnabled = $false
} | ConvertTo-Json -Depth 10

try {
    Invoke-RestMethod -Uri $updateUrl -Method Put -Headers $headers -Body $updateBody -ContentType "application/json" | Out-Null
    Write-Host "[OK] Client configuration updated successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Updated configuration:" -ForegroundColor Cyan
    Write-Host "  - Redirect URIs: * (all URIs allowed)" -ForegroundColor White
    Write-Host "  - Web Origins: + (all origins allowed)" -ForegroundColor White
} catch {
    Write-Host "[ERROR] Failed to update client: $_" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Configuration complete!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "You can now try accessing the frontend with tenant subdomains." -ForegroundColor Yellow
Write-Host "Example: http://tenant1.localhost:5173" -ForegroundColor Yellow


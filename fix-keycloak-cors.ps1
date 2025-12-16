# Script to fix Keycloak CORS configuration
param(
    [string]$KeycloakUrl = "http://localhost:8085",
    [string]$Realm = "kymatic",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin",
    [string]$ClientId = "react-client"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Fixing Keycloak CORS Configuration" -ForegroundColor Cyan
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

# Step 2: Get client configuration
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
        Write-Host "[ERROR] Client '$ClientId' not found" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[OK] Client found: $($client.id)" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to get client: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Get full client representation to preserve all settings
Write-Host ""
Write-Host "Step 3: Getting full client configuration..." -ForegroundColor Yellow
$getClientDetailUrl = "$KeycloakUrl/admin/realms/$Realm/clients/$($client.id)"
try {
    $fullClient = Invoke-RestMethod -Uri $getClientDetailUrl -Method Get -Headers $headers
    Write-Host "[OK] Full client configuration retrieved" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to get full client config: $_" -ForegroundColor Red
    exit 1
}

# Step 4: Update client with CORS-friendly settings
Write-Host ""
Write-Host "Step 4: Updating client CORS configuration..." -ForegroundColor Yellow

# Update webOrigins to allow all origins
$fullClient.webOrigins = @("+")
$fullClient.redirectUris = @("*")
$fullClient.publicClient = $true
$fullClient.enabled = $true
$fullClient.standardFlowEnabled = $true

$updateBody = $fullClient | ConvertTo-Json -Depth 10

try {
    Invoke-RestMethod -Uri $getClientDetailUrl -Method Put -Headers $headers -Body $updateBody -ContentType "application/json" | Out-Null
    Write-Host "[OK] Client CORS configuration updated!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Updated settings:" -ForegroundColor Cyan
    Write-Host "  - Web Origins: + (all origins allowed)" -ForegroundColor White
    Write-Host "  - Redirect URIs: * (all URIs allowed)" -ForegroundColor White
} catch {
    Write-Host "[ERROR] Failed to update client: $_" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
    exit 1
}

# Step 5: Update realm to allow CORS headers
Write-Host ""
Write-Host "Step 5: Updating realm CORS settings..." -ForegroundColor Yellow
$getRealmUrl = "$KeycloakUrl/admin/realms/$Realm"
try {
    $realm = Invoke-RestMethod -Uri $getRealmUrl -Method Get -Headers $headers
    
    # Add HTTP headers configuration for CORS
    if (-not $realm.attributes) {
        $realm | Add-Member -MemberType NoteProperty -Name "attributes" -Value @{}
    }
    
    # Ensure CORS is enabled at realm level
    $realm.attributes."cors.enabled" = "true"
    $realm.attributes."cors.allowed.origins" = "*"
    $realm.attributes."cors.allowed.headers" = "*"
    
    $realmBody = $realm | ConvertTo-Json -Depth 10
    
    Invoke-RestMethod -Uri $getRealmUrl -Method Put -Headers $headers -Body $realmBody -ContentType "application/json" | Out-Null
    Write-Host "[OK] Realm CORS settings updated!" -ForegroundColor Green
} catch {
    Write-Host "[WARNING] Failed to update realm settings (may not be critical): $_" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "CORS Configuration Complete!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "The client should now accept requests from any origin." -ForegroundColor Yellow
Write-Host "Try accessing your app again at: http://shijintest123.localhost:5173" -ForegroundColor Yellow


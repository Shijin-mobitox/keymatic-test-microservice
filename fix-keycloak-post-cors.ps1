# Script to fix Keycloak CORS for POST requests from frontend
param(
    [string]$KeycloakUrl = "http://localhost:8085",
    [string]$Realm = "kymatic",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin",
    [string]$ClientId = "react-client",
    [string]$FrontendOrigin = "http://shijintest123.localhost:5173"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Fixing Keycloak POST CORS Configuration" -ForegroundColor Cyan
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

# Step 4: Update with explicit frontend origin and wildcard
Write-Host ""
Write-Host "Step 3: Updating client with explicit frontend origin..." -ForegroundColor Yellow

# Add both the specific origin and wildcard for maximum compatibility
$webOrigins = @(
    $FrontendOrigin,
    "http://localhost:5173",
    "http://localhost:3000",
    "+"  # Wildcard for all origins
)

# Remove duplicates
$webOrigins = $webOrigins | Select-Object -Unique

$redirectUris = @(
    "$FrontendOrigin/*",
    "$FrontendOrigin/",
    "http://localhost:5173/*",
    "http://localhost:5173/",
    "http://localhost:3000/*",
    "http://localhost:3000/",
    "*"  # Wildcard
)

# Remove duplicates
$redirectUris = $redirectUris | Select-Object -Unique

$fullClient.webOrigins = $webOrigins
$fullClient.redirectUris = $redirectUris
$fullClient.publicClient = $true
$fullClient.enabled = $true
$fullClient.standardFlowEnabled = $true
$fullClient.directAccessGrantsEnabled = $false
$fullClient.implicitFlowEnabled = $false

# Enable CORS for all methods including POST
$fullClient.attributes = @{}
$fullClient.attributes."cors.allowed.methods" = "GET,POST,PUT,DELETE,OPTIONS"
$fullClient.attributes."cors.allowed.headers" = "*"
$fullClient.attributes."cors.exposed.headers" = "*"

$updateBody = $fullClient | ConvertTo-Json -Depth 10

try {
    Invoke-RestMethod -Uri $getClientDetailUrl -Method Put -Headers $headers -Body $updateBody -ContentType "application/json" | Out-Null
    Write-Host "[OK] Client updated successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Web Origins:" -ForegroundColor Cyan
    $webOrigins | ForEach-Object { Write-Host "  - $_" -ForegroundColor White }
    Write-Host ""
    Write-Host "Redirect URIs:" -ForegroundColor Cyan
    $redirectUris | ForEach-Object { Write-Host "  - $_" -ForegroundColor White }
} catch {
    Write-Host "[ERROR] Failed to update: $_" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Configuration Complete!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Restart Keycloak to ensure changes take effect:" -ForegroundColor Yellow
Write-Host "  docker-compose restart keycloak" -ForegroundColor White


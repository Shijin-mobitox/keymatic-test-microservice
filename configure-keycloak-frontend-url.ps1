# Script to configure Keycloak realm frontend URL
# This makes Keycloak use the frontend URL for redirects and form actions
param(
    [string]$KeycloakUrl = "http://localhost:8085",
    [string]$Realm = "kymatic",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin",
    [string]$FrontendUrl = "http://shijintest123.localhost:5173"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Configuring Keycloak Frontend URL" -ForegroundColor Cyan
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

# Step 2: Get realm configuration
Write-Host ""
Write-Host "Step 2: Getting realm configuration..." -ForegroundColor Yellow
$headers = @{
    "Authorization" = "Bearer $adminToken"
    "Content-Type" = "application/json"
}

$getRealmUrl = "$KeycloakUrl/admin/realms/$Realm"
try {
    $realm = Invoke-RestMethod -Uri $getRealmUrl -Method Get -Headers $headers
    Write-Host "[OK] Realm configuration retrieved" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to get realm: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Update realm with frontend URL
Write-Host ""
Write-Host "Step 3: Updating realm frontend URL..." -ForegroundColor Yellow
Write-Host "Setting frontend URL to: $FrontendUrl" -ForegroundColor Gray

# Set the frontend URL in realm attributes
if (-not $realm.attributes) {
    $realm | Add-Member -MemberType NoteProperty -Name "attributes" -Value @{}
}

# Keycloak uses 'frontendUrl' attribute for the frontend URL
$realm.attributes."frontendUrl" = $FrontendUrl

# Also ensure CORS is enabled
$realm.attributes."cors.enabled" = "true"
$realm.attributes."cors.allowed.origins" = "*"

$realmBody = $realm | ConvertTo-Json -Depth 10

try {
    Invoke-RestMethod -Uri $getRealmUrl -Method Put -Headers $headers -Body $realmBody -ContentType "application/json" | Out-Null
    Write-Host "[OK] Realm frontend URL updated!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Frontend URL: $FrontendUrl" -ForegroundColor Cyan
    Write-Host "CORS enabled: true" -ForegroundColor Cyan
} catch {
    Write-Host "[ERROR] Failed to update realm: $_" -ForegroundColor Red
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
Write-Host "Note: You may need to restart Keycloak for changes to take effect:" -ForegroundColor Yellow
Write-Host "  docker-compose restart keycloak" -ForegroundColor White
Write-Host ""
Write-Host "Or clear your browser cache and try logging in again." -ForegroundColor Yellow


# Script to set Keycloak Frontend URL via Admin API
param(
    [string]$KeycloakUrl = "http://localhost:8085",
    [string]$Realm = "kymatic",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin",
    [string]$FrontendUrl = "http://shijintest123.localhost:5173"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Setting Keycloak Frontend URL" -ForegroundColor Cyan
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

# Step 2: Get realm
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
Write-Host "Step 3: Setting frontend URL to: $FrontendUrl" -ForegroundColor Yellow

# Keycloak uses 'frontendUrl' property directly (not in attributes)
$realm.frontendUrl = $FrontendUrl
if (-not $realm.attributes) {
    $realm | Add-Member -MemberType NoteProperty -Name "attributes" -Value @{}
}
$realmBody = $realm | ConvertTo-Json -Depth 10

try {
    Invoke-RestMethod -Uri $getRealmUrl -Method Put -Headers $headers -Body $realmBody -ContentType "application/json" | Out-Null
    Write-Host "[OK] Frontend URL set successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Frontend URL: $FrontendUrl" -ForegroundColor Cyan
} catch {
    Write-Host "[ERROR] Failed to update realm: $_" -ForegroundColor Red
    if ($_.Exception.Response) {
        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "Response: $responseBody" -ForegroundColor Red
        } catch {
            Write-Host "Could not read error response" -ForegroundColor Yellow
        }
    }
    
    Write-Host ""
    Write-Host "MANUAL CONFIGURATION REQUIRED:" -ForegroundColor Yellow
    Write-Host "1. Go to: $KeycloakUrl/admin" -ForegroundColor White
    Write-Host "2. Login: $AdminUser / $AdminPassword" -ForegroundColor White
    Write-Host "3. Navigate to: Realm '$Realm' -> Realm Settings -> General" -ForegroundColor White
    Write-Host "4. Find 'Frontend URL' field" -ForegroundColor White
    Write-Host "5. Set it to: $FrontendUrl" -ForegroundColor White
    Write-Host "6. Click Save" -ForegroundColor White
    exit 1
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Configuration Complete!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Important: Clear browser cookies and cache, then try again!" -ForegroundColor Yellow


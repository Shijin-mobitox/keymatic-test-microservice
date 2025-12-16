# Working script to set Keycloak Frontend URL via Admin API
param(
    [string]$KeycloakUrl = "http://localhost:8085",
    [string]$Realm = "kymatic",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin",
    [string]$FrontendUrl = "http://shijintest123.localhost:5173"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Setting Keycloak Frontend URL (Working)" -ForegroundColor Cyan
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

# Step 2: Get current realm configuration
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

# Step 3: Create a new realm object with frontend URL set
Write-Host ""
Write-Host "Step 3: Updating realm with frontend URL..." -ForegroundColor Yellow
Write-Host "Setting frontend URL to: $FrontendUrl" -ForegroundColor Gray

# Keycloak Admin API expects frontendUrl as a direct property
# We need to add it as a property to the realm object
$realmJson = $realm | ConvertTo-Json -Depth 20
$realmObject = $realmJson | ConvertFrom-Json

# Add frontendUrl property
$realmObject | Add-Member -MemberType NoteProperty -Name "frontendUrl" -Value $FrontendUrl -Force

# Convert back to JSON for API call
$realmBody = $realmObject | ConvertTo-Json -Depth 20 -Compress

# Step 4: Update realm
try {
    $response = Invoke-RestMethod -Uri $getRealmUrl -Method Put -Headers $headers -Body ([System.Text.Encoding]::UTF8.GetBytes($realmBody)) -ContentType "application/json"
    Write-Host "[OK] Realm updated successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Frontend URL has been set to: $FrontendUrl" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "IMPORTANT: Restart Keycloak and clear browser cache!" -ForegroundColor Yellow
    Write-Host "  docker-compose restart keycloak" -ForegroundColor White
} catch {
    Write-Host "[ERROR] Failed to update realm: $_" -ForegroundColor Red
    if ($_.Exception.Response) {
        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "Error details: $responseBody" -ForegroundColor Red
        } catch {
            # Ignore
        }
    }
    
    Write-Host ""
    Write-Host "AUTOMATED UPDATE FAILED. Manual configuration required:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "1. Go to: $KeycloakUrl/admin" -ForegroundColor White
    Write-Host "2. Login: $AdminUser / $AdminPassword" -ForegroundColor White
    Write-Host "3. Navigate: Realm '$Realm' -> Realm Settings -> General" -ForegroundColor White
    Write-Host "4. Find 'Frontend URL' field and set to: $FrontendUrl" -ForegroundColor White
    Write-Host "5. Click Save" -ForegroundColor White
    exit 1
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Configuration Complete!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan


# Script to configure Keycloak hostname settings to accept frontend origin
# This allows cookies to work properly across origins
param(
    [string]$KeycloakUrl = "http://localhost:8085",
    [string]$Realm = "kymatic",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin",
    [string]$FrontendUrl = "http://shijintest123.localhost:5173"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Configuring Keycloak Hostname Settings" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Note: This requires Keycloak to be configured via environment variables" -ForegroundColor Yellow
Write-Host "or admin console. Let's verify the current configuration." -ForegroundColor Yellow
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

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Configuration Instructions" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "The issue is that Keycloak can't find session cookies in cross-origin requests." -ForegroundColor Yellow
Write-Host ""
Write-Host "SOLUTION: Configure Keycloak hostname via Docker environment variables" -ForegroundColor Green
Write-Host ""
Write-Host "Add these environment variables to Keycloak in docker-compose.yml:" -ForegroundColor Cyan
Write-Host "  KC_HOSTNAME_STRICT_HTTPS: false" -ForegroundColor White
Write-Host "  KC_HOSTNAME_STRICT_BACKCHANNEL: false" -ForegroundColor White
Write-Host "  KC_PROXY: edge" -ForegroundColor White
Write-Host ""
Write-Host "Or configure via Admin Console:" -ForegroundColor Cyan
Write-Host "  1. Go to: $KeycloakUrl/admin" -ForegroundColor White
Write-Host "  2. Realm: kymatic -> Realm Settings -> General" -ForegroundColor White
Write-Host "  3. Set 'Frontend URL' to: $FrontendUrl" -ForegroundColor White
Write-Host ""
Write-Host "Then restart Keycloak:" -ForegroundColor Cyan
Write-Host "  docker-compose restart keycloak" -ForegroundColor White


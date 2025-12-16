# Keycloak Token Generator Script
# This script generates JWT access tokens from Keycloak for use with tenant-service

param(
    [string]$Username = "admin",
    [string]$Password = "admin",
    [string]$ClientId = "tenant-service",
    [string]$ClientSecret = "tenant-secret",
    [string]$Realm = "kymatic",
    [string]$KeycloakUrl = "http://localhost:8085"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Keycloak Token Generator" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$tokenUrl = "$KeycloakUrl/realms/$Realm/protocol/openid-connect/token"

Write-Host "Requesting token from: $tokenUrl" -ForegroundColor Yellow
Write-Host "Username: $Username" -ForegroundColor Yellow
Write-Host "Client: $ClientId" -ForegroundColor Yellow
Write-Host ""

try {
    $body = @{
        grant_type = "password"
        client_id = $ClientId
        client_secret = $ClientSecret
        username = $Username
        password = $Password
    } | ConvertTo-Json

    $headers = @{
        "Content-Type" = "application/x-www-form-urlencoded"
    }

    # Convert to form-urlencoded format
    $formData = "grant_type=password&client_id=$ClientId&client_secret=$ClientSecret&username=$Username&password=$Password"

    $response = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $formData -Headers @{"Content-Type" = "application/x-www-form-urlencoded"}

    Write-Host "[OK] Token generated successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Access Token:" -ForegroundColor Cyan
    Write-Host $response.access_token -ForegroundColor White
    Write-Host ""
    Write-Host "Token Type: $($response.token_type)" -ForegroundColor Green
    Write-Host "Expires In: $($response.expires_in) seconds" -ForegroundColor Green
    Write-Host "Refresh Token: $($response.refresh_token)" -ForegroundColor Gray
    Write-Host ""

    # Decode JWT to show claims (without verification)
    $tokenParts = $response.access_token.Split('.')
    if ($tokenParts.Length -eq 3) {
        $payload = $tokenParts[1]
        # Add padding if needed
        while ($payload.Length % 4) {
            $payload += "="
        }
        $jsonBytes = [System.Convert]::FromBase64String($payload)
        $jsonString = [System.Text.Encoding]::UTF8.GetString($jsonBytes)
        $claims = $jsonString | ConvertFrom-Json
        
        Write-Host "Token Claims:" -ForegroundColor Cyan
        Write-Host "  - Subject (sub): $($claims.sub)" -ForegroundColor White
        Write-Host "  - Username (preferred_username): $($claims.preferred_username)" -ForegroundColor White
        Write-Host "  - Email: $($claims.email)" -ForegroundColor White
        Write-Host "  - Tenant ID: $($claims.tenant_id)" -ForegroundColor Yellow
        Write-Host "  - Expires: $([DateTimeOffset]::FromUnixTimeSeconds($claims.exp).LocalDateTime)" -ForegroundColor White
        Write-Host ""
    }

    # Save token to file for easy use
    $tokenFile = "token.txt"
    $response.access_token | Out-File -FilePath $tokenFile -Encoding utf8
    Write-Host "Token saved to: $tokenFile" -ForegroundColor Green
    Write-Host ""
    Write-Host "To use this token, run:" -ForegroundColor Cyan
    Write-Host "  `$token = Get-Content token.txt" -ForegroundColor White
    Write-Host "  Invoke-RestMethod -Uri http://localhost:8083/api/me -Headers @{Authorization = `"Bearer `$token`"}" -ForegroundColor White
    Write-Host ""

    return $response.access_token
}
catch {
    Write-Host "[ERROR] Error generating token:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
    exit 1
}


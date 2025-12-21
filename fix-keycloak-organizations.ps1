# Script to fix Keycloak Organizations feature
param(
    [Parameter(Mandatory=$false)]
    [string]$KeycloakUrl = "http://localhost:8085"
)

Write-Host "=== FIXING KEYCLOAK ORGANIZATIONS FEATURE ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check current Keycloak configuration
Write-Host "Step 1: Checking current Keycloak setup..." -ForegroundColor Yellow

# Get admin token
try {
    $tokenPayload = @{
        grant_type = "password"
        client_id = "admin-cli"
        username = "admin"
        password = "admin"
    }
    
    $tokenResponse = Invoke-RestMethod -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" -Method Post -Body $tokenPayload -ContentType "application/x-www-form-urlencoded"
    $accessToken = $tokenResponse.access_token
    Write-Host "✅ Connected to Keycloak successfully" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to connect to Keycloak: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Make sure Keycloak is running on $KeycloakUrl" -ForegroundColor Yellow
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type" = "application/json"
}

# Step 2: Check if Organizations feature is available
Write-Host "Step 2: Checking Organizations feature availability..." -ForegroundColor Yellow

try {
    $orgsResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/kymatic/organizations" -Method Get -Headers $headers -UseBasicParsing
    Write-Host "✅ Organizations API is accessible" -ForegroundColor Green
    
    $orgs = $orgsResponse.Content | ConvertFrom-Json
    Write-Host "Current organizations count: $($orgs.Count)" -ForegroundColor White
} catch {
    Write-Host "❌ Organizations API not accessible: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "This indicates Organizations feature is not properly enabled" -ForegroundColor Yellow
}

# Step 3: Try to create a test organization to verify functionality
Write-Host "Step 3: Testing organization creation..." -ForegroundColor Yellow

$testOrgPayload = @{
    alias = "test-org-$(Get-Random)"
    name = "Test Organization"
    enabled = $true
} | ConvertTo-Json

try {
    $createOrgResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/kymatic/organizations" -Method Post -Body $testOrgPayload -Headers $headers -UseBasicParsing
    
    if ($createOrgResponse.StatusCode -eq 201) {
        Write-Host "✅ Organization creation works!" -ForegroundColor Green
        
        # Clean up test organization
        $orgLocation = $createOrgResponse.Headers.Location
        $orgId = $orgLocation.Split('/')[-1]
        try {
            Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/kymatic/organizations/$orgId" -Method Delete -Headers $headers -UseBasicParsing | Out-Null
            Write-Host "✅ Test organization cleaned up" -ForegroundColor Green
        } catch {
            Write-Host "⚠️ Could not clean up test organization" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "❌ Organization creation failed!" -ForegroundColor Red
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Error details: $responseBody" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "=== SOLUTION REQUIRED ===" -ForegroundColor Red
    Write-Host "The Organizations feature is not working properly." -ForegroundColor Yellow
    Write-Host "You need to enable it manually in Keycloak Admin Console:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "1. Open: $KeycloakUrl" -ForegroundColor White
    Write-Host "2. Login: admin / admin" -ForegroundColor White
    Write-Host "3. Select 'kymatic' realm" -ForegroundColor White
    Write-Host "4. Go to 'Realm Settings' -> 'Features' tab" -ForegroundColor White
    Write-Host "5. Find 'Organizations' and enable it" -ForegroundColor White
    Write-Host "6. Save and restart Keycloak container:" -ForegroundColor White
    Write-Host "   docker-compose restart keycloak" -ForegroundColor Cyan
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "✅ Keycloak Organizations feature is working correctly!" -ForegroundColor Green
Write-Host "You can now proceed with tenant creation." -ForegroundColor Green

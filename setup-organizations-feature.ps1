# Script to setup Organizations feature following Skycloak best practices
# Based on: https://skycloak.io/blog/multitenancy-in-keycloak-using-the-organizations-feature/

param(
    [Parameter(Mandatory=$false)]
    [string]$KeycloakUrl = "http://localhost:8085",
    
    [Parameter(Mandatory=$false)]
    [string]$RealmName = "kymatic"
)

Write-Host "=== SETTING UP ORGANIZATIONS FEATURE (Skycloak Guide) ===" -ForegroundColor Cyan
Write-Host "Following: https://skycloak.io/blog/multitenancy-in-keycloak-using-the-organizations-feature/" -ForegroundColor White
Write-Host ""

# Step 1: Wait for Keycloak to be ready
Write-Host "Step 1: Waiting for Keycloak 26.2.4 to be ready..." -ForegroundColor Yellow
$maxWait = 120
$waited = 0
do {
    try {
        $healthCheck = Invoke-WebRequest -Uri "$KeycloakUrl" -Method Get -UseBasicParsing -TimeoutSec 5
        if ($healthCheck.StatusCode -eq 200) {
            Write-Host "✅ Keycloak is ready!" -ForegroundColor Green
            break
        }
    } catch {
        Write-Host "⏳ Waiting for Keycloak... ($waited/$maxWait seconds)" -ForegroundColor Yellow
        Start-Sleep -Seconds 10
        $waited += 10
    }
} while ($waited -lt $maxWait)

if ($waited -ge $maxWait) {
    Write-Host "❌ Keycloak did not start within $maxWait seconds" -ForegroundColor Red
    exit 1
}

# Step 2: Get admin access token
Write-Host "Step 2: Getting admin access token..." -ForegroundColor Yellow
try {
    $tokenPayload = @{
        grant_type = "password"
        client_id = "admin-cli"
        username = "admin"
        password = "admin"
    }
    
    $tokenResponse = Invoke-RestMethod -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" -Method Post -Body $tokenPayload -ContentType "application/x-www-form-urlencoded"
    $accessToken = $tokenResponse.access_token
    Write-Host "✅ Got admin access token" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to get admin access token: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type" = "application/json"
}

# Step 3: Check if kymatic realm exists, create if needed
Write-Host "Step 3: Checking kymatic realm..." -ForegroundColor Yellow
try {
    $realmResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$RealmName" -Method Get -Headers $headers -UseBasicParsing
    Write-Host "✅ Realm '$RealmName' exists" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 404) {
        Write-Host "⚠️ Creating kymatic realm..." -ForegroundColor Yellow
        
        $realmPayload = @{
            realm = $RealmName
            enabled = $true
            displayName = "Kymatic Multi-Tenant Realm"
        } | ConvertTo-Json
        
        try {
            $createRealmResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms" -Method Post -Body $realmPayload -Headers $headers -UseBasicParsing
            Write-Host "✅ Realm '$RealmName' created" -ForegroundColor Green
        } catch {
            Write-Host "❌ Failed to create realm: $($_.Exception.Message)" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "❌ Error checking realm: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

# Step 4: Check Organizations feature availability
Write-Host "Step 4: Checking Organizations feature..." -ForegroundColor Yellow
try {
    $orgsResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$RealmName/organizations" -Method Get -Headers $headers -UseBasicParsing
    Write-Host "✅ Organizations feature is available!" -ForegroundColor Green
    $orgs = $orgsResponse.Content | ConvertFrom-Json
    Write-Host "Current organizations: $($orgs.Count)" -ForegroundColor White
} catch {
    Write-Host "❌ Organizations feature not available: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "MANUAL SETUP REQUIRED (following Skycloak guide):" -ForegroundColor Yellow
    Write-Host "1. Open: $KeycloakUrl" -ForegroundColor White
    Write-Host "2. Login: admin / admin" -ForegroundColor White
    Write-Host "3. Select '$RealmName' realm" -ForegroundColor White
    Write-Host "4. Go to 'Realm Settings' -> 'Features' tab" -ForegroundColor White
    Write-Host "5. Enable 'Organizations' feature" -ForegroundColor White
    Write-Host "6. Save and refresh" -ForegroundColor White
    Write-Host "7. 'Organizations' tab should appear in left sidebar" -ForegroundColor White
    Write-Host ""
    Write-Host "After enabling, run this script again to verify." -ForegroundColor Yellow
    exit 1
}

# Step 5: Test organization creation with domain
Write-Host "Step 5: Testing organization creation with domain..." -ForegroundColor Yellow
$testOrgAlias = "test-org-$(Get-Random)"
$testOrgDomain = "$testOrgAlias.local"

$testOrgPayload = @{
    alias = $testOrgAlias
    name = "Test Organization"
    enabled = $true
    domains = @(
        @{
            name = $testOrgDomain
        }
    )
} | ConvertTo-Json -Depth 3

try {
    $createOrgResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$RealmName/organizations" -Method Post -Body $testOrgPayload -Headers $headers -UseBasicParsing
    
    if ($createOrgResponse.StatusCode -eq 201) {
        Write-Host "✅ Organization creation with domain works!" -ForegroundColor Green
        
        # Clean up test organization
        $orgLocation = $createOrgResponse.Headers.Location
        $orgId = $orgLocation.Split('/')[-1]
        try {
            Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$RealmName/organizations/$orgId" -Method Delete -Headers $headers -UseBasicParsing | Out-Null
            Write-Host "✅ Test organization cleaned up" -ForegroundColor Green
        } catch {
            Write-Host "⚠️ Could not clean up test organization" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "❌ Organization creation failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "✅ Organizations feature setup completed!" -ForegroundColor Green
Write-Host "You can now create tenants with proper Organizations functionality." -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Run: .\create-tenant-with-user.ps1 -TenantSlug 'mytenant' -TenantName 'My Tenant'" -ForegroundColor White
Write-Host "2. Check Organizations tab in Keycloak Admin Console" -ForegroundColor White
Write-Host "3. Each tenant will have its own organization with domain (e.g., mytenant.local)" -ForegroundColor White

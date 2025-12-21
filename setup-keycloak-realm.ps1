# Script to automatically setup Keycloak realm with Organizations feature
param(
    [Parameter(Mandatory=$false)]
    [string]$KeycloakUrl = "http://localhost:8085",
    
    [Parameter(Mandatory=$false)]
    [string]$AdminUsername = "admin",
    
    [Parameter(Mandatory=$false)]
    [string]$AdminPassword = "admin",
    
    [Parameter(Mandatory=$false)]
    [string]$RealmName = "kymatic"
)

Write-Host "=== SETTING UP KEYCLOAK REALM WITH ORGANIZATIONS ===" -ForegroundColor Cyan

# Step 1: Get admin access token
Write-Host "Step 1: Getting admin access token..." -ForegroundColor Yellow
try {
    $tokenPayload = @{
        grant_type = "password"
        client_id = "admin-cli"
        username = $AdminUsername
        password = $AdminPassword
    }
    
    $tokenResponse = Invoke-RestMethod -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" -Method Post -Body $tokenPayload -ContentType "application/x-www-form-urlencoded"
    $accessToken = $tokenResponse.access_token
    Write-Host "✅ Got admin access token" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to get admin access token: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Make sure Keycloak is running on $KeycloakUrl" -ForegroundColor Yellow
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type" = "application/json"
}

# Step 2: Check if realm exists
Write-Host "Step 2: Checking if realm '$RealmName' exists..." -ForegroundColor Yellow
try {
    $realmResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$RealmName" -Method Get -Headers $headers -UseBasicParsing
    Write-Host "✅ Realm '$RealmName' already exists" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 404) {
        Write-Host "⚠️ Realm '$RealmName' does not exist. Creating it..." -ForegroundColor Yellow
        
        # Create the realm
        $realmPayload = @{
            realm = $RealmName
            enabled = $true
            displayName = "Kymatic Multi-Tenant Realm"
        } | ConvertTo-Json
        
        try {
            $createRealmResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms" -Method Post -Body $realmPayload -Headers $headers -UseBasicParsing
            if ($createRealmResponse.StatusCode -eq 201) {
                Write-Host "✅ Realm '$RealmName' created successfully" -ForegroundColor Green
            } else {
                Write-Host "❌ Failed to create realm: $($createRealmResponse.StatusCode)" -ForegroundColor Red
                exit 1
            }
        } catch {
            Write-Host "❌ Failed to create realm: $($_.Exception.Message)" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "❌ Error checking realm: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

# Step 3: Check Organizations feature availability
Write-Host "Step 3: Checking Organizations feature..." -ForegroundColor Yellow
try {
    $orgsResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$RealmName/organizations" -Method Get -Headers $headers -UseBasicParsing
    Write-Host "✅ Organizations feature is available and working!" -ForegroundColor Green
    $orgs = $orgsResponse.Content | ConvertFrom-Json
    Write-Host "Current organizations: $($orgs.Count)" -ForegroundColor White
} catch {
    if ($_.Exception.Response.StatusCode -eq 404) {
        Write-Host "❌ Organizations feature is not enabled or not available in Keycloak 25.0.0" -ForegroundColor Red
        Write-Host ""
        Write-Host "MANUAL STEPS REQUIRED:" -ForegroundColor Yellow
        Write-Host "1. Open: $KeycloakUrl" -ForegroundColor White
        Write-Host "2. Login: $AdminUsername / $AdminPassword" -ForegroundColor White
        Write-Host "3. Select '$RealmName' realm" -ForegroundColor White
        Write-Host "4. Go to 'Realm Settings' -> 'Features' tab" -ForegroundColor White
        Write-Host "5. Enable 'Organizations' feature" -ForegroundColor White
        Write-Host "6. Save and refresh the page" -ForegroundColor White
        Write-Host "7. Look for 'Organizations' tab in left sidebar" -ForegroundColor White
        exit 1
    } else {
        Write-Host "❌ Error checking Organizations: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

# Step 4: Test organization creation
Write-Host "Step 4: Testing organization creation..." -ForegroundColor Yellow
$testOrgPayload = @{
    alias = "test-setup-org"
    name = "Test Setup Organization"
    enabled = $true
} | ConvertTo-Json

try {
    $createOrgResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$RealmName/organizations" -Method Post -Body $testOrgPayload -Headers $headers -UseBasicParsing
    
    if ($createOrgResponse.StatusCode -eq 201) {
        Write-Host "✅ Organization creation works!" -ForegroundColor Green
        
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
    Write-Host "❌ Organization creation test failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "✅ Keycloak realm setup completed!" -ForegroundColor Green
Write-Host "You can now test tenant creation - Organizations should work properly." -ForegroundColor Green

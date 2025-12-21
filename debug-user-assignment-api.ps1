# Debug script to test the exact user assignment API call that's failing
param(
    [Parameter(Mandatory=$false)]
    [string]$KeycloakUrl = "http://localhost:8085",
    
    [Parameter(Mandatory=$false)]
    [string]$Realm = "kymatic"
)

Write-Host "=== DEBUGGING USER ASSIGNMENT API ===" -ForegroundColor Cyan

# Step 1: Get admin access token
Write-Host "Step 1: Getting admin access token..." -ForegroundColor Yellow
try {
    $tokenPayload = @{
        grant_type = "password"
        client_id = "admin-cli"
        username = "admin"
        password = "admin"
    }
    
    $tokenResponse = Invoke-RestMethod -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" -Method Post -Body $tokenPayload -ContentType "application/x-www-form-urlencoded"
    $accessToken = $tokenResponse.access_token
    Write-Host "✅ Got access token" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to get access token: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type" = "application/json"
}

# Step 2: List existing organizations to find one to test with
Write-Host "Step 2: Listing existing organizations..." -ForegroundColor Yellow
try {
    $orgsResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/organizations" -Method Get -Headers $headers -UseBasicParsing
    $orgs = $orgsResponse.Content | ConvertFrom-Json
    
    if ($orgs.Count -gt 0) {
        $testOrg = $orgs[0]
        $orgId = $testOrg.id
        $orgAlias = $testOrg.alias
        Write-Host "✅ Found existing organization: $orgAlias (ID: $orgId)" -ForegroundColor Green
    } else {
        Write-Host "❌ No organizations found. Creating a test organization..." -ForegroundColor Yellow
        
        # Create a test organization
        $orgPayload = @{
            alias = "debug-org-$(Get-Random)"
            name = "Debug Organization"
            enabled = $true
        } | ConvertTo-Json
        
        $createOrgResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/organizations" -Method Post -Body $orgPayload -Headers $headers -UseBasicParsing
        
        if ($createOrgResponse.StatusCode -eq 201) {
            $orgLocation = $createOrgResponse.Headers.Location
            $orgId = $orgLocation.Split('/')[-1]
            Write-Host "✅ Created test organization with ID: $orgId" -ForegroundColor Green
        } else {
            Write-Host "❌ Failed to create test organization" -ForegroundColor Red
            exit 1
        }
    }
} catch {
    Write-Host "❌ Failed to list organizations: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 3: Create a test user
Write-Host "Step 3: Creating test user..." -ForegroundColor Yellow
$testEmail = "debuguser$(Get-Random)@test.com"
$userPayload = @{
    username = $testEmail
    email = $testEmail
    firstName = "Debug"
    lastName = "User"
    emailVerified = $true
    enabled = $true
} | ConvertTo-Json

try {
    $createUserResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/users" -Method Post -Body $userPayload -Headers $headers -UseBasicParsing
    
    if ($createUserResponse.StatusCode -eq 201) {
        $userLocation = $createUserResponse.Headers.Location
        $userId = $userLocation.Split('/')[-1]
        Write-Host "✅ Created test user with ID: $userId" -ForegroundColor Green
    } else {
        Write-Host "❌ Failed to create test user" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Failed to create test user: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 4: Wait and verify user exists (same as our production code)
Write-Host "Step 4: Waiting and verifying user exists..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

try {
    $getUserResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/users/$userId" -Method Get -Headers $headers -UseBasicParsing
    if ($getUserResponse.StatusCode -eq 200) {
        Write-Host "✅ User verification successful" -ForegroundColor Green
        $userInfo = $getUserResponse.Content | ConvertFrom-Json
        Write-Host "User details: $($userInfo.username) - $($userInfo.email)" -ForegroundColor White
    } else {
        Write-Host "❌ User verification failed: $($getUserResponse.StatusCode)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ User verification failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 5: Test the exact API call that's failing in production
Write-Host "Step 5: Testing user assignment to organization (exact production API call)..." -ForegroundColor Yellow

# This is the exact payload format used in production
$assignPayload = @{
    id = $userId
} | ConvertTo-Json

Write-Host "Organization ID: $orgId" -ForegroundColor White
Write-Host "User ID: $userId" -ForegroundColor White
Write-Host "Assignment payload: $assignPayload" -ForegroundColor White
Write-Host "API URL: $KeycloakUrl/admin/realms/$Realm/organizations/$orgId/members" -ForegroundColor White

try {
    $assignResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/organizations/$orgId/members" -Method Post -Body $assignPayload -Headers $headers -UseBasicParsing
    
    if ($assignResponse.StatusCode -ge 200 -and $assignResponse.StatusCode -lt 300) {
        Write-Host "✅ SUCCESS! User assigned to organization successfully!" -ForegroundColor Green
        Write-Host "Status Code: $($assignResponse.StatusCode)" -ForegroundColor Green
        Write-Host "This means the API call works - the issue might be elsewhere." -ForegroundColor Yellow
    } else {
        Write-Host "❌ User assignment failed with status: $($assignResponse.StatusCode)" -ForegroundColor Red
        Write-Host "Response: $($assignResponse.Content)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ User assignment failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Status Code: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        try {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $responseBody = $reader.ReadToEnd()
            Write-Host "Response Body: $responseBody" -ForegroundColor Red
            
            # Check if it's the same "User does not exist" error
            if ($responseBody -like "*User does not exist*") {
                Write-Host "" -ForegroundColor Red
                Write-Host "🔍 FOUND THE ISSUE!" -ForegroundColor Red
                Write-Host "The API call is failing with 'User does not exist' even though:" -ForegroundColor Yellow
                Write-Host "1. User was created successfully" -ForegroundColor Yellow
                Write-Host "2. User verification passed" -ForegroundColor Yellow
                Write-Host "3. User exists in Keycloak" -ForegroundColor Yellow
                Write-Host "" -ForegroundColor Yellow
                Write-Host "This suggests a bug in Keycloak 26.2.0 Organizations feature" -ForegroundColor Red
                Write-Host "or a configuration issue with the Organizations API." -ForegroundColor Red
            }
        } catch {
            Write-Host "Could not read error response" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "Debug test completed!" -ForegroundColor Cyan

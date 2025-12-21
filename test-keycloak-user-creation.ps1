# Test script to verify Keycloak user creation without organization assignment
param(
    [Parameter(Mandatory=$false)]
    [string]$KeycloakUrl = "http://localhost:8085",
    
    [Parameter(Mandatory=$false)]
    [string]$AdminUsername = "admin",
    
    [Parameter(Mandatory=$false)]
    [string]$AdminPassword = "admin",
    
    [Parameter(Mandatory=$false)]
    [string]$Realm = "kymatic"
)

Write-Host "Testing Keycloak user creation..." -ForegroundColor Cyan

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
    Write-Host "✅ Got access token" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to get access token: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 2: Create a test user
Write-Host "Step 2: Creating test user..." -ForegroundColor Yellow
$testEmail = "testuser$(Get-Random)@test.com"
$userPayload = @{
    username = $testEmail
    email = $testEmail
    firstName = "Test"
    lastName = "User"
    emailVerified = $true
    enabled = $true
} | ConvertTo-Json

$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type" = "application/json"
}

try {
    $createResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/users" -Method Post -Body $userPayload -Headers $headers -UseBasicParsing
    
    if ($createResponse.StatusCode -eq 201) {
        # Extract user ID from Location header
        $location = $createResponse.Headers.Location
        if ($location) {
            $userId = $location.Split('/')[-1]
            Write-Host "✅ User created successfully with ID: $userId" -ForegroundColor Green
            
            # Step 3: Verify user exists
            Write-Host "Step 3: Verifying user exists..." -ForegroundColor Yellow
            Start-Sleep -Seconds 2
            
            try {
                $getUserResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/users/$userId" -Method Get -Headers $headers -UseBasicParsing
                if ($getUserResponse.StatusCode -eq 200) {
                    Write-Host "✅ User verification successful" -ForegroundColor Green
                    $userInfo = $getUserResponse.Content | ConvertFrom-Json
                    Write-Host "User details: $($userInfo.username) - $($userInfo.email)" -ForegroundColor White
                } else {
                    Write-Host "❌ User verification failed with status: $($getUserResponse.StatusCode)" -ForegroundColor Red
                }
            } catch {
                Write-Host "❌ User verification failed: $($_.Exception.Message)" -ForegroundColor Red
            }
            
            # Step 4: Check if Organizations feature is available
            Write-Host "Step 4: Checking Organizations feature..." -ForegroundColor Yellow
            try {
                $orgsResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/organizations" -Method Get -Headers $headers -UseBasicParsing
                if ($orgsResponse.StatusCode -eq 200) {
                    Write-Host "✅ Organizations feature is available" -ForegroundColor Green
                    $orgs = $orgsResponse.Content | ConvertFrom-Json
                    Write-Host "Found $($orgs.Count) organizations" -ForegroundColor White
                } else {
                    Write-Host "❌ Organizations feature not available or error: $($orgsResponse.StatusCode)" -ForegroundColor Red
                }
            } catch {
                Write-Host "❌ Organizations feature check failed: $($_.Exception.Message)" -ForegroundColor Red
                Write-Host "This might indicate Organizations feature is not enabled in Keycloak" -ForegroundColor Yellow
            }
            
        } else {
            Write-Host "❌ Could not extract user ID from Location header" -ForegroundColor Red
        }
    } else {
        Write-Host "❌ User creation failed with status: $($createResponse.StatusCode)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ User creation failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
}

Write-Host "`nTest completed!" -ForegroundColor Cyan

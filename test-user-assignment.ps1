# Test script to debug user assignment to organization
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

Write-Host "Testing user assignment to organization..." -ForegroundColor Cyan

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

$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type" = "application/json"
}

# Step 2: Create an organization
Write-Host "Step 2: Creating test organization..." -ForegroundColor Yellow
$orgAlias = "testorg$(Get-Random)"
$orgPayload = @{
    alias = $orgAlias
    name = "Test Organization"
    enabled = $true
} | ConvertTo-Json

try {
    $orgResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/organizations" -Method Post -Body $orgPayload -Headers $headers -UseBasicParsing
    
    if ($orgResponse.StatusCode -eq 201) {
        $orgLocation = $orgResponse.Headers.Location
        $orgId = $orgLocation.Split('/')[-1]
        Write-Host "✅ Organization created with ID: $orgId" -ForegroundColor Green
    } else {
        Write-Host "❌ Organization creation failed: $($orgResponse.StatusCode)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Organization creation failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 3: Create a test user
Write-Host "Step 3: Creating test user..." -ForegroundColor Yellow
$testEmail = "testuser$(Get-Random)@test.com"
$userPayload = @{
    username = $testEmail
    email = $testEmail
    firstName = "Test"
    lastName = "User"
    emailVerified = $true
    enabled = $true
} | ConvertTo-Json

try {
    $createResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/users" -Method Post -Body $userPayload -Headers $headers -UseBasicParsing
    
    if ($createResponse.StatusCode -eq 201) {
        $location = $createResponse.Headers.Location
        $userId = $location.Split('/')[-1]
        Write-Host "✅ User created with ID: $userId" -ForegroundColor Green
    } else {
        Write-Host "❌ User creation failed: $($createResponse.StatusCode)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ User creation failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 4: Wait and verify user exists
Write-Host "Step 4: Waiting and verifying user exists..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

try {
    $getUserResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/users/$userId" -Method Get -Headers $headers -UseBasicParsing
    if ($getUserResponse.StatusCode -eq 200) {
        Write-Host "✅ User verification successful" -ForegroundColor Green
    } else {
        Write-Host "❌ User verification failed: $($getUserResponse.StatusCode)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ User verification failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 5: Try to assign user to organization
Write-Host "Step 5: Assigning user to organization..." -ForegroundColor Yellow
$assignPayload = @{
    id = $userId
} | ConvertTo-Json

try {
    $assignResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/organizations/$orgId/members" -Method Post -Body $assignPayload -Headers $headers -UseBasicParsing
    
    if ($assignResponse.StatusCode -ge 200 -and $assignResponse.StatusCode -lt 300) {
        Write-Host "✅ User assigned to organization successfully!" -ForegroundColor Green
    } else {
        Write-Host "❌ User assignment failed with status: $($assignResponse.StatusCode)" -ForegroundColor Red
        Write-Host "Response: $($assignResponse.Content)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ User assignment failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
}

# Cleanup
Write-Host "Cleanup: Deleting test organization..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/organizations/$orgId" -Method Delete -Headers $headers -UseBasicParsing | Out-Null
    Write-Host "✅ Organization deleted" -ForegroundColor Green
} catch {
    Write-Host "⚠️ Failed to delete organization: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`nTest completed!" -ForegroundColor Cyan

# Test script to verify Keycloak Groups API works as alternative to Organizations
$KeycloakUrl = "http://localhost:8085"
$Realm = "kymatic"

Write-Host "Testing Keycloak Groups API as alternative to Organizations..." -ForegroundColor Cyan

# Get admin token
$tokenPayload = @{
    grant_type = "password"
    client_id = "admin-cli"
    username = "admin"
    password = "admin"
}

$tokenResponse = Invoke-RestMethod -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" -Method Post -Body $tokenPayload -ContentType "application/x-www-form-urlencoded"
$accessToken = $tokenResponse.access_token

$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type" = "application/json"
}

# Step 1: Create a test group (equivalent to organization)
Write-Host "Step 1: Creating test group..." -ForegroundColor Yellow
$groupName = "tenant-testorg$(Get-Random)"
$groupPayload = @{
    name = $groupName
    path = "/$groupName"
    attributes = @{
        description = "Test tenant group"
    }
} | ConvertTo-Json

try {
    $createGroupResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/groups" -Method Post -Body $groupPayload -Headers $headers -UseBasicParsing
    
    if ($createGroupResponse.StatusCode -eq 201) {
        $groupLocation = $createGroupResponse.Headers.Location
        $groupId = $groupLocation.Split('/')[-1]
        Write-Host "✅ Group created successfully with ID: $groupId" -ForegroundColor Green
    } else {
        Write-Host "❌ Group creation failed: $($createGroupResponse.StatusCode)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Group creation failed: $($_.Exception.Message)" -ForegroundColor Red
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

try {
    $createUserResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/users" -Method Post -Body $userPayload -Headers $headers -UseBasicParsing
    
    if ($createUserResponse.StatusCode -eq 201) {
        $userLocation = $createUserResponse.Headers.Location
        $userId = $userLocation.Split('/')[-1]
        Write-Host "✅ User created successfully with ID: $userId" -ForegroundColor Green
    } else {
        Write-Host "❌ User creation failed: $($createUserResponse.StatusCode)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ User creation failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 3: Wait and verify user exists
Write-Host "Step 3: Verifying user exists..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

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

# Step 4: Assign user to group (this is the key test)
Write-Host "Step 4: Assigning user to group..." -ForegroundColor Yellow

try {
    # Groups API uses PUT to /admin/realms/{realm}/users/{userId}/groups/{groupId}
    $assignResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/users/$userId/groups/$groupId" -Method Put -Headers $headers -UseBasicParsing
    
    if ($assignResponse.StatusCode -ge 200 -and $assignResponse.StatusCode -lt 300) {
        Write-Host "✅ SUCCESS! User assigned to group successfully!" -ForegroundColor Green
        Write-Host "Status Code: $($assignResponse.StatusCode)" -ForegroundColor Green
        
        # Verify the assignment worked
        Write-Host "Step 5: Verifying group membership..." -ForegroundColor Yellow
        $groupsResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/users/$userId/groups" -Method Get -Headers $headers -UseBasicParsing
        $userGroups = $groupsResponse.Content | ConvertFrom-Json
        
        $foundGroup = $userGroups | Where-Object { $_.id -eq $groupId }
        if ($foundGroup) {
            Write-Host "✅ Group membership verified!" -ForegroundColor Green
            Write-Host "User is member of group: $($foundGroup.name)" -ForegroundColor White
        } else {
            Write-Host "⚠️ Group assignment succeeded but membership not found" -ForegroundColor Yellow
        }
        
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
Write-Host "Cleanup: Deleting test group..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/groups/$groupId" -Method Delete -Headers $headers -UseBasicParsing | Out-Null
    Write-Host "✅ Group deleted" -ForegroundColor Green
} catch {
    Write-Host "⚠️ Failed to delete group: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Groups API test completed!" -ForegroundColor Cyan
Write-Host "If this test succeeded, Groups API is a viable alternative to Organizations API." -ForegroundColor Green

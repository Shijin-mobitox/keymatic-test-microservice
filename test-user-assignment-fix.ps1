# Test the corrected user assignment API payload format
param(
    [Parameter(Mandatory=$false)]
    [string]$KeycloakUrl = "https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io",
    
    [Parameter(Mandatory=$false)]
    [string]$AdminUsername = "admin-gG7X0T1x",
    
    [Parameter(Mandatory=$false)]
    [string]$AdminPassword = "blEzm8bnafcGnv50",
    
    [Parameter(Mandatory=$false)]
    [string]$RealmName = "kymatic"
)

Write-Host "TESTING CORRECTED USER ASSIGNMENT PAYLOAD" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Get admin access token
Write-Host "[1/4] Getting admin access token..." -ForegroundColor Yellow
try {
    $tokenPayload = @{
        grant_type = "password"
        client_id = "admin-cli"
        username = $AdminUsername
        password = $AdminPassword
    }
    
    $tokenResponse = Invoke-RestMethod -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" -Method Post -Body $tokenPayload -ContentType "application/x-www-form-urlencoded"
    $accessToken = $tokenResponse.access_token
    Write-Host "[SUCCESS] Admin token obtained" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to get admin token: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type" = "application/json"
}

# Find test user
Write-Host "[2/4] Finding test user..." -ForegroundColor Yellow
try {
    $users = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$RealmName/users?username=testuser&exact=true" -Method Get -Headers $headers
    
    if ($users.Count -gt 0) {
        $testUserId = $users[0].id
        Write-Host "[SUCCESS] Found test user: $testUserId" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Test user 'testuser' not found. Please create it first." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "[ERROR] Failed to find test user: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Find/create test organization
Write-Host "[3/4] Finding/creating test organization..." -ForegroundColor Yellow
try {
    $orgs = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$RealmName/organizations?alias=test-assignment" -Method Get -Headers $headers
    
    if ($orgs.Count -gt 0) {
        $orgId = $orgs[0].id
        Write-Host "[SUCCESS] Found existing organization: $orgId" -ForegroundColor Green
    } else {
        # Create test organization
        $orgPayload = @{
            alias = "test-assignment"
            name = "Test Assignment Organization"
            enabled = $true
            domains = @(
                @{
                    name = "test-assignment.local"
                }
            )
        } | ConvertTo-Json -Depth 10
        
        $createResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$RealmName/organizations" -Method Post -Body $orgPayload -Headers $headers -UseBasicParsing
        
        if ($createResponse.StatusCode -eq 201) {
            $location = $createResponse.Headers.Location
            $orgId = $location.Split('/')[-1]
            Write-Host "[SUCCESS] Created test organization: $orgId" -ForegroundColor Green
        } else {
            throw "Organization creation failed: $($createResponse.StatusCode)"
        }
    }
} catch {
    Write-Host "[ERROR] Failed to setup test organization: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test user assignment with CORRECTED payload format
Write-Host "[4/4] Testing user assignment with corrected payload..." -ForegroundColor Yellow
Write-Host ""

# Test OLD format (should fail)
Write-Host "Testing OLD format: {\"id\": \"$testUserId\"}" -ForegroundColor White
try {
    $oldPayload = @{
        id = $testUserId
    } | ConvertTo-Json
    
    $oldResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$RealmName/organizations/$orgId/members" -Method Post -Body $oldPayload -Headers $headers -UseBasicParsing -ErrorAction Stop
    
    if ($oldResponse.StatusCode -ge 200 -and $oldResponse.StatusCode -lt 300) {
        Write-Host "[UNEXPECTED] OLD format worked: $($oldResponse.StatusCode)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "[EXPECTED] OLD format failed: $($_.Exception.Response.StatusCode) $($_.Exception.Message)" -ForegroundColor Gray
}

Write-Host ""

# Test NEW format (should work)
Write-Host "Testing NEW format: \"$testUserId\"" -ForegroundColor White
try {
    # Create JSON string payload - just the user ID as a JSON string
    $newPayload = "`"$testUserId`""
    
    Write-Host "Payload being sent: $newPayload" -ForegroundColor Gray
    
    $newResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$RealmName/organizations/$orgId/members" -Method Post -Body $newPayload -Headers $headers -UseBasicParsing -ErrorAction Stop
    
    if ($newResponse.StatusCode -ge 200 -and $newResponse.StatusCode -lt 300) {
        Write-Host "[SUCCESS] NEW format worked: $($newResponse.StatusCode)" -ForegroundColor Green
        
        # Verify assignment
        Start-Sleep -Seconds 2
        $members = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$RealmName/organizations/$orgId/members" -Method Get -Headers $headers
        
        $assigned = $false
        foreach ($member in $members) {
            if ($member.id -eq $testUserId) {
                $assigned = $true
                break
            }
        }
        
        if ($assigned) {
            Write-Host "[VERIFIED] User is now assigned to organization!" -ForegroundColor Green
        } else {
            Write-Host "[WARNING] Assignment API returned success but user not found in members" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "[ERROR] NEW format failed: $($_.Exception.Response.StatusCode) $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response body: $responseBody" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "[COMPLETE] Test completed" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor Yellow
Write-Host "- OLD format: JSON object with id field" -ForegroundColor White
Write-Host "- NEW format: Raw user ID as JSON string" -ForegroundColor White
Write-Host "- The tenant service has been updated to use the NEW format" -ForegroundColor Green

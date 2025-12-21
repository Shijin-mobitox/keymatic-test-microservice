# Simple test for user assignment to organization
$KeycloakUrl = "http://localhost:8085"
$Realm = "kymatic"

Write-Host "Testing user assignment to organization..." -ForegroundColor Cyan

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

# Get existing organizations
$orgsResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/organizations" -Method Get -Headers $headers -UseBasicParsing
$orgs = $orgsResponse.Content | ConvertFrom-Json

if ($orgs.Count -gt 0) {
    $orgId = $orgs[0].id
    Write-Host "Using existing organization: $($orgs[0].alias) (ID: $orgId)" -ForegroundColor Green
    
    # Create a test user
    $testEmail = "testuser$(Get-Random)@test.com"
    $userPayload = @{
        username = $testEmail
        email = $testEmail
        firstName = "Test"
        lastName = "User"
        emailVerified = $true
        enabled = $true
    } | ConvertTo-Json
    
    $createUserResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/users" -Method Post -Body $userPayload -Headers $headers -UseBasicParsing
    
    if ($createUserResponse.StatusCode -eq 201) {
        $userLocation = $createUserResponse.Headers.Location
        $userId = $userLocation.Split('/')[-1]
        Write-Host "Created user with ID: $userId" -ForegroundColor Green
        
        # Wait a bit
        Start-Sleep -Seconds 3
        
        # Verify user exists
        try {
            $getUserResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/users/$userId" -Method Get -Headers $headers -UseBasicParsing
            Write-Host "User verification: SUCCESS" -ForegroundColor Green
        } catch {
            Write-Host "User verification: FAILED" -ForegroundColor Red
            exit 1
        }
        
        # Try to assign user to organization
        $assignPayload = @{ id = $userId } | ConvertTo-Json
        
        Write-Host "Attempting user assignment..." -ForegroundColor Yellow
        Write-Host "URL: $KeycloakUrl/admin/realms/$Realm/organizations/$orgId/members" -ForegroundColor White
        Write-Host "Payload: $assignPayload" -ForegroundColor White
        
        try {
            $assignResponse = Invoke-WebRequest -Uri "$KeycloakUrl/admin/realms/$Realm/organizations/$orgId/members" -Method Post -Body $assignPayload -Headers $headers -UseBasicParsing
            Write-Host "SUCCESS! User assigned to organization" -ForegroundColor Green
            Write-Host "Status: $($assignResponse.StatusCode)" -ForegroundColor Green
        } catch {
            Write-Host "FAILED! User assignment error" -ForegroundColor Red
            Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
            
            if ($_.Exception.Response) {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $responseBody = $reader.ReadToEnd()
                Write-Host "Response: $responseBody" -ForegroundColor Red
                
                if ($responseBody -like "*User does not exist*") {
                    Write-Host "" -ForegroundColor Yellow
                    Write-Host "CONFIRMED: Same error as production!" -ForegroundColor Red
                    Write-Host "This is a Keycloak Organizations API issue." -ForegroundColor Red
                }
            }
        }
    } else {
        Write-Host "Failed to create user" -ForegroundColor Red
    }
} else {
    Write-Host "No organizations found" -ForegroundColor Red
}

Write-Host "Test completed" -ForegroundColor Cyan

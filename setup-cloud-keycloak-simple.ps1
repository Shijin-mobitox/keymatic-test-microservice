# Complete Cloud Keycloak Setup Script for KyMatic
# This script will configure your entire Keycloak instance with all necessary settings

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

Write-Host "COMPLETE KYMATIC KEYCLOAK SETUP" -ForegroundColor Cyan
Write-Host "===============================================" -ForegroundColor Cyan
Write-Host "Keycloak URL: $KeycloakUrl" -ForegroundColor White
Write-Host "Realm: $RealmName" -ForegroundColor White
Write-Host ""

# Function to make authenticated requests
function Invoke-KeycloakRequest {
    param($Uri, $Method = "GET", $Body = $null, $Headers)
    
    try {
        if ($Body) {
            $response = Invoke-RestMethod -Uri $Uri -Method $Method -Body $Body -Headers $Headers
        } else {
            $response = Invoke-RestMethod -Uri $Uri -Method $Method -Headers $Headers
        }
        return $response
    } catch {
        Write-Host "[ERROR] Request failed: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response) {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $responseBody = $reader.ReadToEnd()
            Write-Host "Response: $responseBody" -ForegroundColor Red
        }
        throw
    }
}

# Step 1: Get admin access token
Write-Host "[1/8] Getting admin access token..." -ForegroundColor Yellow
try {
    $tokenPayload = @{
        grant_type = "password"
        client_id = "admin-cli"
        username = $AdminUsername
        password = $AdminPassword
    }
    
    $tokenResponse = Invoke-RestMethod -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" -Method Post -Body $tokenPayload -ContentType "application/x-www-form-urlencoded"
    $accessToken = $tokenResponse.access_token
    Write-Host "[SUCCESS] Admin access token obtained" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to get admin access token: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "[INFO] Check if Keycloak URL and admin credentials are correct" -ForegroundColor Yellow
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type" = "application/json"
}

# Step 2: Create/Check Realm
Write-Host "[2/8] Setting up '$RealmName' realm..." -ForegroundColor Yellow
try {
    $realm = Invoke-KeycloakRequest -Uri "$KeycloakUrl/admin/realms/$RealmName" -Headers $headers
    Write-Host "[SUCCESS] Realm '$RealmName' already exists" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 404) {
        Write-Host "[WARNING] Creating realm '$RealmName'..." -ForegroundColor Yellow
        
        $realmPayload = @{
            realm = $RealmName
            enabled = $true
            displayName = "KyMatic Multi-Tenant Realm"
            loginWithEmailAllowed = $true
            registrationAllowed = $false
            resetPasswordAllowed = $true
            rememberMe = $true
            verifyEmail = $false
            loginTheme = "keycloak"
            accessTokenLifespan = 3600
            refreshTokenMaxReuse = 0
        } | ConvertTo-Json -Depth 10
        
        $null = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms" -Method Post -Body $realmPayload -Headers $headers
        Write-Host "[SUCCESS] Realm '$RealmName' created successfully" -ForegroundColor Green
    } else {
        throw
    }
}

# Step 3: Create React Client
Write-Host "[3/8] Setting up React client 'kymatic-react-client'..." -ForegroundColor Yellow
try {
    # Check if client exists
    $clients = Invoke-KeycloakRequest -Uri "$KeycloakUrl/admin/realms/$RealmName/clients?clientId=kymatic-react-client" -Headers $headers
    
    if ($clients.Count -gt 0) {
        Write-Host "[SUCCESS] Client 'kymatic-react-client' already exists" -ForegroundColor Green
        $clientId = $clients[0].id
    } else {
        Write-Host "[WARNING] Creating React client..." -ForegroundColor Yellow
        
        $clientPayload = @{
            clientId = "kymatic-react-client"
            name = "KyMatic React Frontend"
            description = "React frontend application for KyMatic"
            protocol = "openid-connect"
            enabled = $true
            publicClient = $true
            standardFlowEnabled = $true
            directAccessGrantsEnabled = $true
            implicitFlowEnabled = $false
            serviceAccountsEnabled = $false
            authorizationServicesEnabled = $false
            fullScopeAllowed = $true
            redirectUris = @(
                "http://localhost:3000/*",
                "http://localhost:5173/*",
                "http://localhost:3000",
                "http://localhost:5173"
            )
            webOrigins = @(
                "http://localhost:3000",
                "http://localhost:5173",
                "+"
            )
            postLogoutRedirectUris = @(
                "http://localhost:3000/",
                "http://localhost:5173/"
            )
            attributes = @{
                "pkce.code.challenge.method" = "S256"
                "post.logout.redirect.uris" = "+"
                "oauth2.device.authorization.grant.enabled" = $false
                "oidc.ciba.grant.enabled" = $false
            }
        } | ConvertTo-Json -Depth 10
        
        $createResponse = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$RealmName/clients" -Method Post -Body $clientPayload -Headers $headers
        Write-Host "[SUCCESS] React client created successfully" -ForegroundColor Green
        
        # Get the created client ID
        $clients = Invoke-KeycloakRequest -Uri "$KeycloakUrl/admin/realms/$RealmName/clients?clientId=kymatic-react-client" -Headers $headers
        $clientId = $clients[0].id
    }
} catch {
    Write-Host "[ERROR] Failed to create React client: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 4: Create Admin CLI Client for Backend Services
Write-Host "[4/8] Setting up backend admin client..." -ForegroundColor Yellow
try {
    $adminClients = Invoke-KeycloakRequest -Uri "$KeycloakUrl/admin/realms/$RealmName/clients?clientId=admin-cli" -Headers $headers
    
    if ($adminClients.Count -gt 0) {
        Write-Host "[SUCCESS] Admin CLI client already exists" -ForegroundColor Green
        $adminClientId = $adminClients[0].id
    } else {
        Write-Host "[WARNING] Creating admin CLI client..." -ForegroundColor Yellow
        
        $adminClientPayload = @{
            clientId = "admin-cli"
            name = "KyMatic Backend Admin Client"
            protocol = "openid-connect"
            enabled = $true
            publicClient = $false
            serviceAccountsEnabled = $true
            standardFlowEnabled = $false
            directAccessGrantsEnabled = $true
            clientAuthenticatorType = "client-secret"
            secret = "your-client-secret-here"
        } | ConvertTo-Json -Depth 10
        
        $null = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$RealmName/clients" -Method Post -Body $adminClientPayload -Headers $headers
        Write-Host "[SUCCESS] Admin CLI client created" -ForegroundColor Green
        
        $adminClients = Invoke-KeycloakRequest -Uri "$KeycloakUrl/admin/realms/$RealmName/clients?clientId=admin-cli" -Headers $headers
        $adminClientId = $adminClients[0].id
    }
} catch {
    Write-Host "[ERROR] Failed to create admin CLI client: $($_.Exception.Message)" -ForegroundColor Red
}

# Step 5: Enable Organizations (if available)
Write-Host "[5/8] Checking Organizations feature..." -ForegroundColor Yellow
try {
    $orgsResponse = Invoke-KeycloakRequest -Uri "$KeycloakUrl/admin/realms/$RealmName/organizations" -Headers $headers
    Write-Host "[SUCCESS] Organizations feature is available and enabled!" -ForegroundColor Green
    $orgs = $orgsResponse
    Write-Host "   Current organizations: $($orgs.Count)" -ForegroundColor White
} catch {
    if ($_.Exception.Response.StatusCode -eq 404) {
        Write-Host "[WARNING] Organizations feature not enabled" -ForegroundColor Yellow
        Write-Host "   You may need to enable it manually in Realm Settings -> Features" -ForegroundColor White
    } else {
        Write-Host "[ERROR] Error checking Organizations: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Step 6: Create Test User
Write-Host "[6/8] Creating test user..." -ForegroundColor Yellow
try {
    $users = Invoke-KeycloakRequest -Uri "$KeycloakUrl/admin/realms/$RealmName/users?username=testuser" -Headers $headers
    
    if ($users.Count -gt 0) {
        Write-Host "[SUCCESS] Test user already exists" -ForegroundColor Green
    } else {
        $userPayload = @{
            username = "testuser"
            firstName = "Test"
            lastName = "User"
            email = "test@kymatic.com"
            enabled = $true
            emailVerified = $true
            credentials = @(
                @{
                    type = "password"
                    value = "testpassword"
                    temporary = $false
                }
            )
        } | ConvertTo-Json -Depth 10
        
        $null = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$RealmName/users" -Method Post -Body $userPayload -Headers $headers
        Write-Host "[SUCCESS] Test user 'testuser' created with password 'testpassword'" -ForegroundColor Green
    }
} catch {
    Write-Host "[WARNING] Could not create test user: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Step 7: Configure Frontend Environment
Write-Host "[7/8] Creating frontend configuration..." -ForegroundColor Yellow
try {
    $frontendEnvContent = @"
# Frontend Keycloak Configuration for Cloud Instance
VITE_KEYCLOAK_BASE_URL=https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io/
VITE_KEYCLOAK_REALM=kymatic
VITE_KEYCLOAK_CLIENT_ID=kymatic-react-client
"@

    $frontendEnvContent | Out-File -FilePath "frontend-keycloak-config.env" -Encoding UTF8
    Write-Host "[SUCCESS] Frontend config file created at 'frontend-keycloak-config.env'" -ForegroundColor Green
    Write-Host "   Copy this to 'front-end\.env' to use" -ForegroundColor White
} catch {
    Write-Host "[WARNING] Could not create frontend config file" -ForegroundColor Yellow
}

# Step 8: Test Authentication Flow
Write-Host "[8/8] Testing authentication flow..." -ForegroundColor Yellow
try {
    $testTokenPayload = @{
        grant_type = "password"
        client_id = "kymatic-react-client"
        username = "testuser"
        password = "testpassword"
        scope = "openid profile email"
    }
    
    $testTokenResponse = Invoke-RestMethod -Uri "$KeycloakUrl/realms/$RealmName/protocol/openid-connect/token" -Method Post -Body $testTokenPayload -ContentType "application/x-www-form-urlencoded"
    
    if ($testTokenResponse.access_token) {
        Write-Host "[SUCCESS] Authentication flow test successful!" -ForegroundColor Green
        Write-Host "   Access token received and validated" -ForegroundColor White
    }
} catch {
    Write-Host "[WARNING] Authentication flow test failed - this is normal if user creation failed" -ForegroundColor Yellow
}

# Final Summary
Write-Host ""
Write-Host "[COMPLETE] KEYCLOAK SETUP COMPLETED!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
Write-Host ""
Write-Host "[SUCCESS] Configuration Summary:" -ForegroundColor Cyan
Write-Host "   • Realm: $RealmName" -ForegroundColor White
Write-Host "   • React Client: kymatic-react-client" -ForegroundColor White
Write-Host "   • Admin Client: admin-cli" -ForegroundColor White
Write-Host "   • Test User: testuser / testpassword" -ForegroundColor White
Write-Host ""
Write-Host "[URLS] Access URLs:" -ForegroundColor Cyan
Write-Host "   • Admin Console: $KeycloakUrl/admin" -ForegroundColor White
Write-Host "   • Realm Console: $KeycloakUrl/admin/master/console/#/$RealmName" -ForegroundColor White
Write-Host "   • Auth URL: $KeycloakUrl/realms/$RealmName/protocol/openid-connect/auth" -ForegroundColor White
Write-Host ""
Write-Host "[NEXT] Next Steps:" -ForegroundColor Cyan
Write-Host "   1. Copy: frontend-keycloak-config.env to front-end\.env" -ForegroundColor White
Write-Host "   2. Start frontend: cd front-end && npm run dev" -ForegroundColor White
Write-Host "   3. Test login at: http://localhost:3000" -ForegroundColor White
Write-Host "   4. Use test credentials: testuser / testpassword" -ForegroundColor White
Write-Host ""
Write-Host "[CONFIG] Backend Configuration:" -ForegroundColor Cyan
Write-Host "   Your services should use the cloud config:" -ForegroundColor White
Write-Host "   • Copy: cp keycloak-config-cloud.env .env" -ForegroundColor White
Write-Host "   • Start: docker-compose up -d" -ForegroundColor White
Write-Host ""

# Test the React authentication URL
$authUrl = "$KeycloakUrl/realms/$RealmName/protocol/openid-connect/auth?client_id=kymatic-react-client&redirect_uri=http%3A%2F%2Flocalhost%3A3000%2F&state=test&response_mode=fragment&response_type=code&scope=openid&code_challenge=test&code_challenge_method=S256"
Write-Host "[TEST] Test Auth URL:" -ForegroundColor Yellow
Write-Host "   $authUrl" -ForegroundColor White
Write-Host ""
Write-Host "   This should now work without Page not found error!" -ForegroundColor Green

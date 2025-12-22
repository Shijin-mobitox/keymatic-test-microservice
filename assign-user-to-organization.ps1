# PowerShell Script: Assign User to Organization in Keycloak
# This script handles the Keycloak Organizations API timing issues with proper retry logic

param(
    [Parameter(Mandatory=$true)]
    [string]$OrganizationSlug,
    
    [Parameter(Mandatory=$true)]
    [string]$UserEmail,
    
    [string]$KeycloakUrl = "http://localhost:8085",
    [string]$Realm = "kymatic",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "admin",
    [int]$MaxAttempts = 10,
    [int]$DelaySeconds = 10
)

Write-Host "🎯 Keycloak User-Organization Assignment Script" -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green
Write-Host ""
Write-Host "📊 Parameters:" -ForegroundColor Blue
Write-Host "   Organization: $OrganizationSlug" -ForegroundColor Cyan
Write-Host "   User Email: $UserEmail" -ForegroundColor Cyan
Write-Host "   Keycloak URL: $KeycloakUrl" -ForegroundColor Cyan
Write-Host "   Max Attempts: $MaxAttempts" -ForegroundColor Cyan
Write-Host ""

try {
    # Step 1: Get Admin Token
    Write-Host "🔑 Getting admin token..." -ForegroundColor Cyan
    $tokenResponse = Invoke-RestMethod -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" `
        -Method Post `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{
            grant_type = "password"
            client_id = "admin-cli" 
            username = $AdminUsername
            password = $AdminPassword
        } `
        -TimeoutSec 10
    
    $adminToken = $tokenResponse.access_token
    Write-Host "✅ Admin token obtained!" -ForegroundColor Green
    
    # Step 2: Find Organization
    Write-Host "🔍 Finding organization '$OrganizationSlug'..." -ForegroundColor Cyan
    $orgs = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$Realm/organizations?alias=$OrganizationSlug" `
        -Headers @{ "Authorization" = "Bearer $adminToken" } `
        -TimeoutSec 10
    
    if ($orgs.Count -eq 0) {
        Write-Host "❌ Organization '$OrganizationSlug' not found!" -ForegroundColor Red
        exit 1
    }
    
    $orgId = $orgs[0].id
    $orgName = $orgs[0].name
    Write-Host "✅ Found organization: $orgName (ID: $orgId)" -ForegroundColor Green
    
    # Step 3: Find User
    Write-Host "🔍 Finding user '$UserEmail'..." -ForegroundColor Cyan
    $users = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$Realm/users?email=$UserEmail&exact=true" `
        -Headers @{ "Authorization" = "Bearer $adminToken" } `
        -TimeoutSec 10
    
    if ($users.Count -eq 0) {
        Write-Host "❌ User '$UserEmail' not found!" -ForegroundColor Red
        exit 1
    }
    
    $userId = $users[0].id
    $username = $users[0].username
    Write-Host "✅ Found user: $username (ID: $userId)" -ForegroundColor Green
    
    # Step 4: Check if already assigned
    Write-Host "🔍 Checking current membership..." -ForegroundColor Cyan
    $members = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$Realm/organizations/$orgId/members" `
        -Headers @{ "Authorization" = "Bearer $adminToken" } `
        -TimeoutSec 10
    
    $isAlreadyMember = $members | Where-Object { $_.id -eq $userId }
    if ($isAlreadyMember) {
        Write-Host "✅ User is already a member of the organization!" -ForegroundColor Green
        exit 0
    }
    
    Write-Host "⚠️ User is NOT a member. Attempting assignment..." -ForegroundColor Yellow
    
    # Step 5: Attempt Assignment with Retries
    $assignmentPayload = @{ id = $userId } | ConvertTo-Json -Compress
    $assignmentSucceeded = $false
    
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        try {
            Write-Host "🔄 Assignment attempt $attempt of $MaxAttempts..." -ForegroundColor Cyan
            
            if ($attempt -gt 1) {
                Write-Host "   ⏳ Waiting $DelaySeconds seconds for Keycloak indexing..." -ForegroundColor Yellow
                Start-Sleep -Seconds $DelaySeconds
            }
            
            # Try assignment
            $response = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$Realm/organizations/$orgId/members" `
                -Method Post `
                -Headers @{ 
                    "Authorization" = "Bearer $adminToken"
                    "Content-Type" = "application/json" 
                } `
                -Body $assignmentPayload `
                -TimeoutSec 30
            
            Write-Host "   📊 API Response: Success" -ForegroundColor Green
            
            # Verify assignment actually worked
            Start-Sleep -Seconds 2
            $verifyMembers = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$Realm/organizations/$orgId/members" `
                -Headers @{ "Authorization" = "Bearer $adminToken" } `
                -TimeoutSec 10
            
            $isNowMember = $verifyMembers | Where-Object { $_.id -eq $userId }
            if ($isNowMember) {
                Write-Host "   ✅ Assignment verified successful!" -ForegroundColor Green
                $assignmentSucceeded = $true
                break
            } else {
                Write-Host "   ⚠️ API returned success but verification failed" -ForegroundColor Yellow
            }
            
        } catch {
            $errorMessage = $_.Exception.Message
            if ($_.Exception.Response) {
                $statusCode = $_.Exception.Response.StatusCode
                Write-Host "   ❌ Attempt $attempt failed: HTTP $statusCode" -ForegroundColor Red
                Write-Host "   📋 Error: $errorMessage" -ForegroundColor Gray
                
                # Don't retry on certain error codes
                if ($statusCode -eq 404 -or $statusCode -eq 403) {
                    Write-Host "   🚫 Non-retryable error. Stopping attempts." -ForegroundColor Red
                    break
                }
            } else {
                Write-Host "   ❌ Attempt $attempt failed: $errorMessage" -ForegroundColor Red
            }
        }
    }
    
    # Final Result
    if ($assignmentSucceeded) {
        Write-Host ""
        Write-Host "🎉 SUCCESS! User '$UserEmail' assigned to organization '$OrganizationSlug'" -ForegroundColor Green
        Write-Host "✅ You can now see the user in the organization members list" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "❌ AUTOMATIC ASSIGNMENT FAILED" -ForegroundColor Red
        Write-Host "🛠️ MANUAL GUI METHOD REQUIRED:" -ForegroundColor Yellow
        Write-Host "   1. Go to: $KeycloakUrl" -ForegroundColor Cyan
        Write-Host "   2. Login: $AdminUsername / $AdminPassword" -ForegroundColor Cyan
        Write-Host "   3. Realm: $Realm -> Organizations -> $OrganizationSlug -> Members" -ForegroundColor Cyan
        Write-Host "   4. Add member: $UserEmail" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "📝 This is due to a known Keycloak 26.x Organizations API timing bug" -ForegroundColor Blue
    }
    
} catch {
    Write-Host ""
    Write-Host "❌ SCRIPT ERROR: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "🛠️ Use manual GUI method instead" -ForegroundColor Yellow
}

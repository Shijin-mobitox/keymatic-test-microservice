# PowerShell Script: Clean Up Orphaned Users in Keycloak
# This script finds and removes users that were created during failed tenant attempts

param(
    [string]$KeycloakUrl = "http://localhost:8085",
    [string]$Realm = "kymatic", 
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "admin",
    [string]$EmailPattern = "*@*", # Pattern to match orphaned users
    [switch]$DryRun = $false # Set to $true to see what would be deleted without actually deleting
)

Write-Host "🧹 Keycloak Orphaned Users Cleanup Script" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
Write-Host ""
Write-Host "📊 Parameters:" -ForegroundColor Blue
Write-Host "   Keycloak URL: $KeycloakUrl" -ForegroundColor Cyan
Write-Host "   Realm: $Realm" -ForegroundColor Cyan
Write-Host "   Email Pattern: $EmailPattern" -ForegroundColor Cyan
Write-Host "   Dry Run: $DryRun" -ForegroundColor Cyan
Write-Host ""

try {
    # Get admin token
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
    
    # Get all users
    Write-Host "🔍 Finding users in realm '$Realm'..." -ForegroundColor Cyan
    $allUsers = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$Realm/users?max=1000" `
        -Headers @{ "Authorization" = "Bearer $adminToken" } `
        -TimeoutSec 15
    
    Write-Host "📊 Found $($allUsers.Count) total users" -ForegroundColor Blue
    
    # Filter users that match pattern and might be orphaned
    $suspiciousUsers = $allUsers | Where-Object { 
        $_.email -like $EmailPattern -and 
        $_.username -ne "admin" -and
        ($_.email -like "*test*" -or $_.email -like "*org*" -or $_.email -like "*sdf*")
    }
    
    Write-Host "🔍 Found $($suspiciousUsers.Count) potentially orphaned users:" -ForegroundColor Yellow
    
    foreach($user in $suspiciousUsers) {
        Write-Host "   - $($user.username) (ID: $($user.id))" -ForegroundColor Gray
        Write-Host "     Email: $($user.email)" -ForegroundColor Cyan
        Write-Host "     Created: $([DateTimeOffset]::FromUnixTimeMilliseconds($user.createdTimestamp).ToString('yyyy-MM-dd HH:mm:ss'))" -ForegroundColor Gray
        
        if (-not $DryRun) {
            try {
                Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$Realm/users/$($user.id)" `
                    -Method Delete `
                    -Headers @{ "Authorization" = "Bearer $adminToken" } `
                    -TimeoutSec 10
                Write-Host "     ✅ DELETED" -ForegroundColor Green
            } catch {
                Write-Host "     ❌ DELETE FAILED: $($_.Exception.Message)" -ForegroundColor Red
            }
        } else {
            Write-Host "     🔍 WOULD BE DELETED (dry run)" -ForegroundColor Yellow
        }
        Write-Host ""
    }
    
    if ($DryRun) {
        Write-Host "🔍 DRY RUN COMPLETE" -ForegroundColor Blue
        Write-Host "   Run without -DryRun to actually delete these users" -ForegroundColor Cyan
    } else {
        Write-Host "✅ CLEANUP COMPLETE" -ForegroundColor Green
        Write-Host "   $($suspiciousUsers.Count) potentially orphaned users processed" -ForegroundColor Cyan
    }
    
} catch {
    Write-Host "❌ SCRIPT ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

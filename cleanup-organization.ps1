# PowerShell script to clean up existing organization in Keycloak
# Usage: .\cleanup-organization.ps1 -OrganizationAlias "testorg5"

param(
    [Parameter(Mandatory=$true)]
    [string]$OrganizationAlias,
    
    [string]$KeycloakUrl = "http://localhost:8085",
    [string]$Realm = "kymatic",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin"
)

Write-Host "Cleaning up organization: $OrganizationAlias" -ForegroundColor Yellow

try {
    # Step 1: Get admin access token
    Write-Host "Getting admin access token..." -ForegroundColor Cyan
    
    $tokenBody = @{
        'client_id' = 'admin-cli'
        'username' = $AdminUser
        'password' = $AdminPassword
        'grant_type' = 'password'
    }
    
    $tokenResponse = Invoke-RestMethod -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" -Method Post -Body $tokenBody -ContentType "application/x-www-form-urlencoded"
    $accessToken = $tokenResponse.access_token
    
    Write-Host "Got access token successfully" -ForegroundColor Green
    
    # Step 2: Find organization by alias
    Write-Host "Finding organization by alias: $OrganizationAlias..." -ForegroundColor Cyan
    
    $headers = @{
        'Authorization' = "Bearer $accessToken"
        'Content-Type' = 'application/json'
    }
    
    $orgSearchUrl = "$KeycloakUrl/admin/realms/$Realm/organizations?alias=$OrganizationAlias"
    $organizations = Invoke-RestMethod -Uri $orgSearchUrl -Method Get -Headers $headers
    
    if ($organizations -and $organizations.Count -gt 0) {
        $orgId = $organizations[0].id
        Write-Host "Found organization with ID: $orgId" -ForegroundColor Green
        
        # Step 3: Delete organization
        Write-Host "Deleting organization: $orgId..." -ForegroundColor Cyan
        
        $deleteUrl = "$KeycloakUrl/admin/realms/$Realm/organizations/$orgId"
        Invoke-RestMethod -Uri $deleteUrl -Method Delete -Headers $headers
        
        Write-Host "Successfully deleted organization: $OrganizationAlias" -ForegroundColor Green
        Write-Host "You can now create the tenant again with the same alias." -ForegroundColor Yellow
    } else {
        Write-Host "Organization not found: $OrganizationAlias" -ForegroundColor Yellow
        Write-Host "It may have already been deleted or never existed." -ForegroundColor Gray
    }
    
} catch {
    Write-Host "Error cleaning up organization: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        Write-Host "Response status: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "Cleanup completed successfully!" -ForegroundColor Green
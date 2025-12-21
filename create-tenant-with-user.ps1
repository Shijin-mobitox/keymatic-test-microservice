# PowerShell script to create a tenant with admin user using the TenantOnboardingService
# This creates users in Keycloak (not just the local database)
# Usage: .\create-tenant-with-user.ps1 -TenantSlug "testorg6" -TenantName "Test Organization 6" -AdminEmail "admin@testorg6.com" -AdminPassword "SecurePass123!"

param(
    [Parameter(Mandatory=$true)]
    [string]$TenantSlug,
    
    [Parameter(Mandatory=$true)]
    [string]$TenantName,
    
    [Parameter(Mandatory=$false)]
    [string]$AdminEmail = "admin@$TenantSlug.com",
    
    [Parameter(Mandatory=$false)]
    [string]$AdminPassword = "SecurePass123!",
    
    [Parameter(Mandatory=$false)]
    [string]$AdminFirstName = "Admin",
    
    [Parameter(Mandatory=$false)]
    [string]$AdminLastName = "User",
    
    [Parameter(Mandatory=$false)]
    [string]$SubscriptionTier = "starter",
    
    [Parameter(Mandatory=$false)]
    [int]$MaxUsers = 10,
    
    [Parameter(Mandatory=$false)]
    [int]$MaxStorageGb = 10,
    
    [Parameter(Mandatory=$false)]
    [string]$TenantServiceUrl = "http://localhost:8083"
)

Write-Host "Creating tenant with admin user in Keycloak: $TenantSlug" -ForegroundColor Cyan
Write-Host "Tenant Name: $TenantName" -ForegroundColor White
Write-Host "Admin Email: $AdminEmail" -ForegroundColor White
Write-Host "Service URL: $TenantServiceUrl" -ForegroundColor White

# Create the proper payload structure for CreateTenantRequest
$payload = @{
    tenantName = $TenantName
    slug = $TenantSlug
    subscriptionTier = $SubscriptionTier
    maxUsers = $MaxUsers
    maxStorageGb = $MaxStorageGb
    adminUser = @{
        email = $AdminEmail
        password = $AdminPassword
        firstName = $AdminFirstName
        lastName = $AdminLastName
        emailVerified = $true
    }
    metadata = @{}
} | ConvertTo-Json -Depth 3

Write-Host "`nPayload:" -ForegroundColor Yellow
Write-Host $payload -ForegroundColor Gray

Write-Host "`nCreating tenant..." -ForegroundColor Yellow

try {
    $headers = @{
        "Content-Type" = "application/json"
    }
    
    $response = Invoke-RestMethod -Uri "$TenantServiceUrl/api/tenants" -Method Post -Body $payload -Headers $headers
    
    Write-Host "`n✅ Tenant created successfully!" -ForegroundColor Green
    Write-Host "`nTenant Details:" -ForegroundColor Cyan
    Write-Host "  Tenant ID: $($response.tenantId)" -ForegroundColor White
    Write-Host "  Tenant Name: $($response.tenantName)" -ForegroundColor White
    Write-Host "  Slug: $($response.slug)" -ForegroundColor White
    Write-Host "  Organization ID: $($response.organizationId)" -ForegroundColor White
    Write-Host "  Database: $($response.databaseName)" -ForegroundColor White
    Write-Host "  Status: $($response.status)" -ForegroundColor White
    Write-Host "`nAdmin User Details:" -ForegroundColor Cyan
    Write-Host "  User ID: $($response.adminUserId)" -ForegroundColor White
    Write-Host "  Email: $($response.adminEmail)" -ForegroundColor White
    Write-Host "  Password: $AdminPassword" -ForegroundColor White
    Write-Host "`n🎉 You can now login with the admin credentials!" -ForegroundColor Green
    
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorBody = ""
    
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $errorBody = $reader.ReadToEnd()
        $reader.Close()
        $stream.Close()
    } catch {
        $errorBody = "Could not read error response"
    }
    
    Write-Host "`n❌ Error creating tenant:" -ForegroundColor Red
    Write-Host "Status Code: $statusCode" -ForegroundColor Red
    Write-Host "Error Response: $errorBody" -ForegroundColor Red
    
    if ($statusCode -eq 409) {
        Write-Host "`n💡 Suggestion: The tenant or organization might already exist. Try a different slug." -ForegroundColor Yellow
    } elseif ($statusCode -eq 400) {
        Write-Host "`n💡 Suggestion: Check the payload format and required fields." -ForegroundColor Yellow
    } else {
        Write-Host "`n💡 Suggestion: Check if the tenant-service is running on $TenantServiceUrl" -ForegroundColor Yellow
    }
    
    exit 1
}

Write-Host "`nDone!" -ForegroundColor Green

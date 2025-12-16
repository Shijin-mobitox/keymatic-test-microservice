# PowerShell script to create a default admin user for a tenant
# Usage: .\create-default-user.ps1 -TenantSlug "shijintest123" -Email "admin@test.com" -Password "adming"

param(
    [Parameter(Mandatory=$true)]
    [string]$TenantSlug,
    
    [Parameter(Mandatory=$false)]
    [string]$Email = "admin@test.com",
    
    [Parameter(Mandatory=$false)]
    [string]$Password = "adming",
    
    [Parameter(Mandatory=$false)]
    [string]$TenantServiceUrl = "http://localhost:8083"
)

Write-Host "Creating default admin user for tenant: $TenantSlug" -ForegroundColor Cyan

# Step 1: Get tenant by slug to get tenant UUID
Write-Host "`nStep 1: Fetching tenant information..." -ForegroundColor Yellow
try {
    $tenantResponse = Invoke-RestMethod -Uri "$TenantServiceUrl/api/tenants/slug/$TenantSlug" -Method Get
    $tenantId = $tenantResponse.tenantId
    Write-Host "Found tenant: $($tenantResponse.tenantName) (ID: $tenantId)" -ForegroundColor Green
} catch {
    Write-Host "Error fetching tenant: $_" -ForegroundColor Red
    Write-Host "Make sure the tenant exists and tenant-service is running on $TenantServiceUrl" -ForegroundColor Yellow
    exit 1
}

# Step 2: Create user
Write-Host "`nStep 2: Creating admin user..." -ForegroundColor Yellow
$userPayload = @{
    tenantId = $tenantId
    email = $Email
    password = $Password
    role = "admin"
    isActive = $true
} | ConvertTo-Json

try {
    $headers = @{
        "Content-Type" = "application/json"
        "X-Tenant-ID" = $TenantSlug
    }
    
    $userResponse = Invoke-RestMethod -Uri "$TenantServiceUrl/api/tenant-users" -Method Post -Body $userPayload -Headers $headers
    Write-Host "User created successfully!" -ForegroundColor Green
    Write-Host "`nUser Details:" -ForegroundColor Cyan
    Write-Host "  Email: $Email" -ForegroundColor White
    Write-Host "  Password: $Password" -ForegroundColor White
    Write-Host "  Role: admin" -ForegroundColor White
    Write-Host "  Tenant: $TenantSlug" -ForegroundColor White
    Write-Host "`nYou can now login with these credentials!" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "User with email $Email already exists for this tenant." -ForegroundColor Yellow
        Write-Host "To update the user, use the PUT endpoint or delete and recreate." -ForegroundColor Yellow
    } else {
        Write-Host "Error creating user: $_" -ForegroundColor Red
        Write-Host "Response: $($_.Exception.Response)" -ForegroundColor Red
        exit 1
    }
}

Write-Host "`nDone!" -ForegroundColor Green


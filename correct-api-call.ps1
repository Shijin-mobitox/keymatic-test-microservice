# Correct API call format for Organizations feature
# Use this instead of the legacy adminEmail format

$payload = @{
    tenantName = "orgtenant346"
    slug = "orgtenant346"
    subscriptionTier = "starter"
    maxUsers = 10
    maxStorageGb = 10
    adminUser = @{
        email = "admin@orgtenant346.com"
        password = "SecurePass123!"
        firstName = "Admin"
        lastName = "User"
        emailVerified = $true
    }
    metadata = @{}
} | ConvertTo-Json -Depth 3

$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer YOUR_JWT_TOKEN_HERE"
}

Write-Host "Using correct API format for Organizations..." -ForegroundColor Cyan
Write-Host "Payload:" -ForegroundColor Yellow
Write-Host $payload -ForegroundColor Gray

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8083/api/tenants" -Method Post -Body $payload -Headers $headers
    Write-Host "✅ SUCCESS!" -ForegroundColor Green
    Write-Host "Tenant ID: $($response.tenantId)" -ForegroundColor White
    Write-Host "Organization ID: $($response.organizationId)" -ForegroundColor White
    Write-Host "Admin User ID: $($response.adminUserId)" -ForegroundColor White
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
}

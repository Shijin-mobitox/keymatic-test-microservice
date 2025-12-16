# Create nwind123 tenant
# You need to get a fresh JWT token from http://localhost:3000 first

Write-Host "=== Create nwind123 Tenant ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Please paste your JWT token from the browser (from http://localhost:3000):" -ForegroundColor Yellow
$token = Read-Host

if ([string]::IsNullOrWhiteSpace($token)) {
    Write-Host "Error: Token is required" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

$body = @{
    tenantName = "Northwind 123"
    slug = "nwind123"
    subscriptionTier = "premium"
    maxUsers = 100
    maxStorageGb = 500
    adminEmail = "admin@admin.com"
} | ConvertTo-Json

Write-Host "Creating tenant nwind123..." -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8083/api/tenants" -Headers $headers -Method Post -Body $body -ErrorAction Stop
    Write-Host "✓ Tenant created successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Response:" -ForegroundColor Cyan
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
    Write-Host ""
    Write-Host "Default admin credentials:" -ForegroundColor Yellow
    Write-Host "  Email: admin@admin.com" -ForegroundColor White
    Write-Host "  Password: adming" -ForegroundColor White
} catch {
    Write-Host "✗ Failed to create tenant" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
}


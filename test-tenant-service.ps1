# Test Tenant Service with JWT Token
# This script tests the tenant-service endpoints using a JWT token

param(
    [string]$Token,
    [string]$TokenFile = "token.txt",
    [string]$BaseUrl = "http://localhost:8083"
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Tenant Service API Tester" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Get token
if (-not $Token) {
    if (Test-Path $TokenFile) {
        $Token = Get-Content $TokenFile -Raw | ForEach-Object { $_.Trim() }
        Write-Host "Using token from: $TokenFile" -ForegroundColor Green
    } else {
        Write-Host "Error: No token provided and token file not found." -ForegroundColor Red
        Write-Host "Please run .\get-token.ps1 first or provide -Token parameter" -ForegroundColor Yellow
        exit 1
    }
}

$headers = @{
    "Authorization" = "Bearer $Token"
    "Content-Type" = "application/json"
}

Write-Host "Testing endpoints with token..." -ForegroundColor Yellow
Write-Host ""

# Test 1: Get current user info
Write-Host "1. GET /api/me - Get current user info" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/api/me" -Method Get -Headers $headers
    Write-Host "   ✓ Success!" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 3 | Write-Host
    Write-Host ""
} catch {
    Write-Host "   ✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 2: Get current tenant
Write-Host "2. GET /api/tenant/current - Get current tenant ID" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/api/tenant/current" -Method Get -Headers $headers
    Write-Host "   ✓ Success!" -ForegroundColor Green
    Write-Host "   $response" -ForegroundColor White
    Write-Host ""
} catch {
    Write-Host "   ✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 3: Get tenant info
Write-Host "3. GET /api/tenant/info - Get tenant information" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/api/tenant/info" -Method Get -Headers $headers
    Write-Host "   ✓ Success!" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 3 | Write-Host
    Write-Host ""
} catch {
    Write-Host "   ✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test 4: List tenants (requires admin or specific permissions)
Write-Host "4. GET /api/tenants - List all tenants" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$BaseUrl/api/tenants" -Method Get -Headers $headers
    Write-Host "   ✓ Success!" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 3 | Write-Host
    Write-Host ""
} catch {
    Write-Host "   ✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response.StatusCode -eq 403) {
        Write-Host "   (This endpoint may require admin permissions)" -ForegroundColor Yellow
    }
    Write-Host ""
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Testing complete!" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan


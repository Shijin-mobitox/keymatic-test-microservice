# Test API Endpoints Script
# This script tests the /api/me and /api/tenant/info endpoints

Write-Host "=== Testing Tenant Service Endpoints ===" -ForegroundColor Cyan
Write-Host ""

# Your JWT token from the curl command
$token = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJDNUotWE9PemtYTFJQblNuQ01zRHc4Rl8wbE5QbnFrTFVob3c0dFRIN3prIn0.eyJleHAiOjE3NjQyMzgzODQsImlhdCI6MTc2NDIzODA4NCwiYXV0aF90aW1lIjoxNzY0MjM4MDQ1LCJqdGkiOiI1NjhlNWI3YS04NDhlLTQwOWQtYTQ0Ni1jMTY4Y2M4ZmY5NDUiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODUvcmVhbG1zL2t5bWF0aWMiLCJzdWIiOiI1YTQ4OTkyNC1mOWJlLTQ4ZGUtYWY1YS0yMzQ5Y2ZmOGFkYWQiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJyZWFjdC1jbGllbnQiLCJub25jZSI6IjViYmRhZGVjLTc1ZDMtNDhjYi1hN2I1LWZiNjBiNDMyZDQ0YSIsInNlc3Npb25fc3RhdGUiOiI3ZGZmN2VmMC03N2Q3LTRmOTgtODE3Mi04MThkYjBmODhjMDEiLCJhY3IiOiIwIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHA6Ly9sb2NhbGhvc3Q6MzAwMCIsImh0dHA6Ly9sb2NhbGhvc3Q6NTE3MyIsImh0dHA6Ly9zaGlqaW50ZXN0MTIzLmxvY2FsaG9zdDo1MTczIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJhZG1pbiJdfSwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSIsInNpZCI6IjdkZmY3ZWYwLTc3ZDctNGY5OC04MTcyLTgxOGRiMGY4OGMwMSIsInRlbmFudF9pZCI6InRlbmFudDEiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6IkFkbWluIFVzZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJhZG1pbiIsImdpdmVuX25hbWUiOiJBZG1pbiIsImZhbWlseV9uYW1lIjoiVXNlciIsImVtYWlsIjoiYWRtaW5Aa3ltYXRpYy5jb20ifQ.Q9Z22n9NexQrDUUu_E5mryhJ08bqO52gaJrr96XVWN1VnVE5i2UOKonJqkc6WGLwBeOsyWi_csYVQAmFREbxTngkh2Dt3THuCxmEL3DVOO5pC0MvXmt-_jp7R1dwFWTre3juEIioMvvAiZ9V6SKkO_Iw3RJ4i5qIpqVqrmvVCeVmjdDEuY6ewXDn_VHQkmxtMRQtGeqX-LcvEz3BS-S5cgUiQtSCcHMFf0Prb5M_4QIWZO9uCVLxQjcxjJoYuu4dpzbVer1DB6Y9bWYbKxbYTZ7tp6aN5xOIBz_E1HqsQOYplMoLBSQEdAZdRJc76KEC-0hRI6LREvwdkJl-TIWF9Q"

$headers = @{
    "Authorization" = "Bearer $token"
    "X-Tenant-ID" = "tenant1"
    "Content-Type" = "application/json"
}

# Test 1: /api/me
Write-Host "1. Testing /api/me..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8083/api/me" -Headers $headers -Method Get -ErrorAction Stop
    Write-Host "   ✓ Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "   Response:" -ForegroundColor Green
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
} catch {
    Write-Host "   ✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        Write-Host "   Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    }
}

Write-Host ""

# Test 2: /api/tenant/info
Write-Host "2. Testing /api/tenant/info..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8083/api/tenant/info" -Headers $headers -Method Get -ErrorAction Stop
    Write-Host "   ✓ Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "   Response:" -ForegroundColor Green
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
} catch {
    Write-Host "   ✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        Write-Host "   Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    }
}

Write-Host ""

# Test 3: /api/tenant/current
Write-Host "3. Testing /api/tenant/current..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8083/api/tenant/current" -Headers $headers -Method Get -ErrorAction Stop
    Write-Host "   ✓ Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "   Response: $($response.Content)" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        Write-Host "   Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== Test Complete ===" -ForegroundColor Cyan


# PowerShell script to get a valid token for tenant-service and test the /api/me endpoint

# Generate a random value for testing
$randomValue = -join ((65..90) + (97..122) | Get-Random -Count 8 | % {[char]$_})

# Get token for tenant-service client
$body = @{
    grant_type = "password"
    client_id = "tenant-service"
    client_secret = "tenant-secret"
    username = "admin"  # Replace with your username
    password = "admin"  # Replace with your password
}

$response = Invoke-RestMethod -Uri "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" `
    -Method Post `
    -ContentType "application/x-www-form-urlencoded" `
    -Body $body

$token = $response.access_token
Write-Host "Access Token: $token"
Write-Host "Random Value (testing): $randomValue"

# Test the token against tenant-service
$headers = @{ Authorization = "Bearer $token"; "X-Random-Value" = $randomValue }
try {
    $result = Invoke-RestMethod -Uri "http://localhost:8083/api/me" -Headers $headers
    Write-Host "API Response:"
    $result | ConvertTo-Json
} catch {
    Write-Host "API call failed: $($_.Exception.Message)"
}

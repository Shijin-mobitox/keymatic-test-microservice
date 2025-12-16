# Set tenant_id attribute for admin user via Keycloak REST API
$keycloakUrl = "http://localhost:8085"
$realm = "kymatic"
$adminToken = "<paste your admin token here>"
$tenantIdValue = "tenant1"

# Get user ID for 'admin'
$response = Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$realm/users?username=admin" -Headers @{Authorization = "Bearer $adminToken"}
$userId = $response[0].id
Write-Host "User ID: $userId"

# Set tenant_id attribute
$body = @{
    attributes = @{
        tenant_id = @($tenantIdValue)
    }
} | ConvertTo-Json

try {
    $result = Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$realm/users/$userId" `
        -Method Put `
        -Headers @{Authorization = "Bearer $adminToken"; "Content-Type" = "application/json"} `
        -Body $body
    Write-Host "Attribute update result: $result"
} catch {
    Write-Host "Error: $($_.Exception.Message)"
}

# Generate a fake JWT token with custom claims for local testing (NOT for production)
# This token is unsigned and only for mock backend testing

$header = @{ alg = "none"; typ = "JWT" } | ConvertTo-Json -Compress
$payload = @{ 
  sub = "test-user"
  name = "Test User"
  email = "test@example.com"
  tenant_id = "tenant1"
  custom_claim = "customValue123"
  exp = [int]((Get-Date).AddHours(1) - (Get-Date "1970-01-01T00:00:00Z")).TotalSeconds
} | ConvertTo-Json -Compress

function Base64UrlEncode($str) {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($str)
    $base64 = [Convert]::ToBase64String($bytes)
    $base64.Replace('+', '-').Replace('/', '_').Replace('=', '')
}

$jwt = (Base64UrlEncode $header) + "." + (Base64UrlEncode $payload) + "."
Write-Host "Fake JWT for testing: $jwt"

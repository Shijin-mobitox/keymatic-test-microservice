# Test Keycloak Token Exchange Endpoint
# This script demonstrates how to exchange an authorization code for an access token

Write-Host "=== Keycloak Token Exchange Test ===" -ForegroundColor Green
Write-Host ""

# Configuration
$keycloakUrl = "http://localhost:8085"
$realm = "kymatic"
$clientId = "react-client"
$redirectUri = "http://localhost:3000/"

$tokenEndpoint = "$keycloakUrl/realms/$realm/protocol/openid-connect/token"

Write-Host "Token Endpoint: $tokenEndpoint" -ForegroundColor Cyan
Write-Host ""

# Note: To test this properly, you need:
# 1. A valid authorization code from the login flow
# 2. The matching code_verifier (from PKCE)
# 3. The code must be fresh (not expired)

Write-Host "To get a valid authorization code:" -ForegroundColor Yellow
Write-Host "1. Open browser and go to: http://localhost:3000"
Write-Host "2. Login with Keycloak"
Write-Host "3. Check browser network tab for the redirect with 'code' parameter"
Write-Host "4. Copy the code and code_verifier from the frontend"
Write-Host ""

# Example request (with placeholder values)
Write-Host "Example Request:" -ForegroundColor Yellow
$exampleBody = @{
    grant_type = "authorization_code"
    client_id = $clientId
    redirect_uri = $redirectUri
    code = "YOUR_AUTHORIZATION_CODE_HERE"
    code_verifier = "YOUR_CODE_VERIFIER_HERE"
}

Write-Host "Body parameters:" -ForegroundColor Cyan
$exampleBody.GetEnumerator() | ForEach-Object {
    Write-Host "  $($_.Key) = $($_.Value)"
}
Write-Host ""

# Test with actual values if provided
if ($args.Length -ge 2) {
    $authCode = $args[0]
    $codeVerifier = $args[1]
    
    Write-Host "Testing with provided code..." -ForegroundColor Green
    
    $body = @{
        grant_type = "authorization_code"
        client_id = $clientId
        redirect_uri = $redirectUri
        code = $authCode
        code_verifier = $codeVerifier
    }
    
    try {
        $response = Invoke-RestMethod -Uri $tokenEndpoint `
            -Method Post `
            -ContentType "application/x-www-form-urlencoded" `
            -Body $body
        
        Write-Host "`n✅ Success! Token received:" -ForegroundColor Green
        Write-Host "Access Token (first 50 chars): $($response.access_token.Substring(0, [Math]::Min(50, $response.access_token.Length)))..."
        Write-Host "Token Type: $($response.token_type)"
        Write-Host "Expires In: $($response.expires_in) seconds"
        Write-Host "Refresh Token: $($response.refresh_token.Substring(0, [Math]::Min(50, $response.refresh_token.Length)))..."
        
        # Decode JWT to see claims
        Write-Host "`nDecoding JWT token..." -ForegroundColor Cyan
        $tokenParts = $response.access_token.Split('.')
        if ($tokenParts.Length -eq 3) {
            $payload = $tokenParts[1]
            # Add padding if needed
            while ($payload.Length % 4) {
                $payload += "="
            }
            $bytes = [System.Convert]::FromBase64String($payload)
            $json = [System.Text.Encoding]::UTF8.GetString($bytes)
            $claims = $json | ConvertFrom-Json
            Write-Host "Token Claims:" -ForegroundColor Yellow
            Write-Host "  Issuer: $($claims.iss)"
            Write-Host "  Subject: $($claims.sub)"
            Write-Host "  Username: $($claims.preferred_username)"
            Write-Host "  Email: $($claims.email)"
            Write-Host "  Tenant ID: $($claims.tenant_id)"
        }
        
    } catch {
        Write-Host "`n❌ Error: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "Response: $responseBody" -ForegroundColor Red
        }
    }
} else {
    Write-Host "Usage: .\test-token-exchange.ps1 <authorization_code> <code_verifier>" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Or test manually with curl:" -ForegroundColor Yellow
    Write-Host "curl -X POST `"$tokenEndpoint`" \"
    Write-Host "  -H `"Content-Type: application/x-www-form-urlencoded`" \"
    Write-Host "  -d `"grant_type=authorization_code&client_id=$clientId&redirect_uri=$redirectUri&code=YOUR_CODE&code_verifier=YOUR_VERIFIER`""
}


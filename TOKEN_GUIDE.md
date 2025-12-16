# Access Token Guide for Tenant Service

This guide explains how to generate JWT access tokens from Keycloak and use them to authenticate with the tenant-service.

## Prerequisites

1. **Keycloak must be running** on `http://localhost:8085`
2. **Tenant Service must be running** on `http://localhost:8083`
3. **PowerShell** (for Windows) or **curl** (for Linux/Mac)

## Keycloak Configuration

- **Realm**: `kymatic`
- **Client ID**: `tenant-service`
- **Client Secret**: `tenant-secret`
- **Token Endpoint**: `http://localhost:8085/realms/kymatic/protocol/openid-connect/token`

## Pre-configured Users

| Username | Password | Tenant ID | Email |
|----------|----------|-----------|-------|
| `admin` | `admin` | `tenant1` | admin@kymatic.com |
| `user1` | `password` | `tenant1` | user1@tenant1.com |
| `user2` | `password` | `tenant2` | user2@tenant2.com |

## Method 1: Using PowerShell Script (Recommended for Windows)

### Step 1: Generate Access Token

```powershell
# Generate token for user1 (tenant1)
.\get-token.ps1 -Username user1 -Password password

# Generate token for user2 (tenant2)
.\get-token.ps1 -Username user2 -Password password

# Generate token for admin
.\get-token.ps1 -Username admin -Password admin
```

The script will:
- Request a token from Keycloak
- Display the token and its claims (including `tenant_id`)
- Save the token to `token.txt` for easy reuse

### Step 2: Test Tenant Service with Token

```powershell
# Test endpoints using the saved token
.\test-tenant-service.ps1

# Or use a specific token
.\test-tenant-service.ps1 -Token "your-token-here"
```

## Method 2: Using curl (Linux/Mac/Windows)

### Step 1: Generate Access Token

```bash
curl -X POST "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=tenant-service" \
  -d "client_secret=tenant-secret" \
  -d "username=user1" \
  -d "password=password"
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "...",
  "token_type": "Bearer",
  "not-before-policy": 0,
  "session_state": "...",
  "scope": "profile email"
}
```

### Step 2: Use Token with Tenant Service

```bash
# Extract token from response (save it to a variable)
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ..."

# Test /api/me endpoint
curl -X GET "http://localhost:8083/api/me" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"

# Test /api/tenant/current endpoint
curl -X GET "http://localhost:8083/api/tenant/current" \
  -H "Authorization: Bearer $TOKEN"

# Test /api/tenant/info endpoint
curl -X GET "http://localhost:8083/api/tenant/info" \
  -H "Authorization: Bearer $TOKEN"
```

## Method 3: Using Postman or Similar Tools

### Step 1: Get Token

1. **Request Type**: POST
2. **URL**: `http://localhost:8085/realms/kymatic/protocol/openid-connect/token`
3. **Headers**: 
   - `Content-Type: application/x-www-form-urlencoded`
4. **Body** (x-www-form-urlencoded):
   - `grant_type`: `password`
   - `client_id`: `tenant-service`
   - `client_secret`: `tenant-secret`
   - `username`: `user1`
   - `password`: `password`

5. Copy the `access_token` from the response

### Step 2: Use Token

1. **Request Type**: GET
2. **URL**: `http://localhost:8083/api/me`
3. **Headers**:
   - `Authorization: Bearer <your-access-token>`
   - `Content-Type: application/json`

## Method 4: Using Swagger UI

1. Open Swagger UI: `http://localhost:8083/swagger-ui.html`
2. Click the **"Authorize"** button (top right)
3. In the "bearer-jwt" section, paste your access token
4. Click **"Authorize"** then **"Close"**
5. Now you can test all endpoints directly from Swagger UI

## Understanding the Token

The JWT token contains a `tenant_id` claim that is automatically extracted by the tenant-service. You can decode the token to see its contents:

### Decode JWT Token (PowerShell)

```powershell
$token = "your-token-here"
$parts = $token.Split('.')
$payload = $parts[1]
# Add padding if needed
while ($payload.Length % 4) { $payload += "=" }
$jsonBytes = [System.Convert]::FromBase64String($payload)
$jsonString = [System.Text.Encoding]::UTF8.GetString($jsonBytes)
$claims = $jsonString | ConvertFrom-Json
$claims | ConvertTo-Json
```

### Decode JWT Token (Online)

Use [jwt.io](https://jwt.io) to decode and inspect the token:
1. Paste your token
2. View the payload section
3. Look for the `tenant_id` claim

## Example Token Payload

```json
{
  "sub": "user-id-here",
  "email_verified": true,
  "name": "User One",
  "preferred_username": "user1",
  "given_name": "User",
  "family_name": "One",
  "email": "user1@tenant1.com",
  "tenant_id": "tenant1",  // <-- This is extracted by JwtTenantResolver
  "realm_access": {
    "roles": ["user"]
  },
  "exp": 1234567890,
  "iat": 1234567890
}
```

## Available Endpoints

### Public Endpoints (No Authentication Required)
- `GET /health` - Health check
- `GET /test` - Test endpoint
- `GET /v3/api-docs` - OpenAPI documentation
- `GET /swagger-ui.html` - Swagger UI

### Protected Endpoints (Require JWT Token)

#### Tenant-Aware Endpoints
- `GET /api/me` - Get current user and tenant information
- `GET /api/tenant/current` - Get current tenant ID
- `GET /api/tenant/info` - Get tenant information

#### Tenant Management Endpoints
- `GET /api/tenants` - List all tenants
- `POST /api/tenants` - Create a new tenant
- `GET /api/tenants/{tenantId}` - Get tenant by ID
- `GET /api/tenants/slug/{slug}` - Get tenant by slug
- `POST /api/tenants/{tenantId}/status` - Update tenant status
- `POST /api/tenants/{tenantId}/migrations/run` - Run tenant migrations
- `GET /api/tenants/{tenantId}/migrations` - List tenant migrations

## Troubleshooting

### Error: "401 Unauthorized"
- **Cause**: Invalid or expired token
- **Solution**: Generate a new token using `get-token.ps1`

### Error: "403 Forbidden"
- **Cause**: User doesn't have required permissions
- **Solution**: Use a user with appropriate roles (e.g., admin user)

### Error: "No tenant ID provided"
- **Cause**: Token doesn't contain `tenant_id` claim
- **Solution**: Ensure the user has a `tenant_id` attribute set in Keycloak

### Error: "Connection refused" to Keycloak
- **Cause**: Keycloak is not running
- **Solution**: Start Keycloak with `docker-compose up -d keycloak`

### Token Expired
- **Cause**: Tokens expire after 5 minutes (300 seconds) by default
- **Solution**: 
  - Generate a new token, or
  - Use the refresh token to get a new access token:
    ```bash
    curl -X POST "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "grant_type=refresh_token" \
      -d "client_id=tenant-service" \
      -d "client_secret=tenant-secret" \
      -d "refresh_token=<your-refresh-token>"
    ```

## Quick Start Example

```powershell
# 1. Generate token
.\get-token.ps1 -Username user1 -Password password

# 2. Test the service
.\test-tenant-service.ps1

# 3. Or use directly in PowerShell
$token = Get-Content token.txt
Invoke-RestMethod -Uri "http://localhost:8083/api/me" `
  -Headers @{Authorization = "Bearer $token"}
```

## Security Notes

1. **Never commit tokens to version control**
2. **Tokens expire after 5 minutes** - regenerate as needed
3. **Use HTTPS in production** - current setup is for development only
4. **Store tokens securely** - don't log or expose them
5. **Rotate client secrets** - change `tenant-secret` in production

## Next Steps

- Configure additional users in Keycloak
- Set up different tenant IDs for different users
- Implement role-based access control (RBAC)
- Configure token expiration times
- Set up refresh token rotation


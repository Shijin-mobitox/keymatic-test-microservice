# Quick Start Guide - KyMaticServiceV1

## 🚀 Services Running

All microservices are now running via Docker Compose:

| Service | Port | URL |
|---------|------|-----|
| **Keycloak** | 8085 | http://localhost:8085 |
| **Gateway Service** | 8081 | http://localhost:8081 |
| **Auth Service** | 8082 | http://localhost:8082 |
| **Tenant Service** | 8083 | http://localhost:8083 |
| **API Service** | 8084 | http://localhost:8084 |

## 🔐 Keycloak Access

- **Admin Console**: http://localhost:8085
- **Username**: `admin`
- **Password**: `admin`
- **Realm**: `kymatic` (auto-imported)

## 🧪 Test Users

The realm includes pre-configured test users:

| Username | Password | Tenant ID |
|----------|----------|-----------|
| `user1` | `password` | `tenant1` |
| `user2` | `password` | `tenant2` |
| `admin` | `admin` | `tenant1` |

## 📝 Getting an Access Token

### Using curl (PowerShell):

```powershell
# Get token for user1 (tenant1)
$response = Invoke-RestMethod -Uri "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" `
    -Method Post `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        grant_type = "password"
        client_id = "tenant-service"
        client_secret = "tenant-secret"
        username = "user1"
        password = "password"
    }

$token = $response.access_token
Write-Host "Token: $token"
```

### Using curl (Bash/Linux):

```bash
# Get token for user1 (tenant1)
TOKEN=$(curl -X POST http://localhost:8085/realms/kymatic/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=tenant-service" \
  -d "client_secret=tenant-secret" \
  -d "username=user1" \
  -d "password=password" | jq -r '.access_token')

echo "Token: $TOKEN"
```

## 🧪 Testing the Services

### 1. Test Tenant Service

```powershell
# Get token
$token = (Invoke-RestMethod -Uri "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" `
    -Method Post -ContentType "application/x-www-form-urlencoded" `
    -Body @{grant_type="password";client_id="tenant-service";client_secret="tenant-secret";username="user1";password="password"}).access_token

# Call /api/me endpoint
Invoke-RestMethod -Uri "http://localhost:8083/api/me" `
    -Headers @{Authorization="Bearer $token"}
```

### 2. Test API Service

```powershell
# Get token
$token = (Invoke-RestMethod -Uri "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" `
    -Method Post -ContentType "application/x-www-form-urlencoded" `
    -Body @{grant_type="password";client_id="api-service";client_secret="api-secret";username="user1";password="password"}).access_token

# Call /api/me endpoint
Invoke-RestMethod -Uri "http://localhost:8084/api/me" `
    -Headers @{Authorization="Bearer $token"}
```

### 3. Test Auth Service

```powershell
# Get token
$token = (Invoke-RestMethod -Uri "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" `
    -Method Post -ContentType "application/x-www-form-urlencoded" `
    -Body @{grant_type="password";client_id="tenant-service";client_secret="tenant-secret";username="user1";password="password"}).access_token

# Call /api/me endpoint
Invoke-RestMethod -Uri "http://localhost:8082/api/me" `
    -Headers @{Authorization="Bearer $token"}
```

### 4. Test Gateway Service

```powershell
# Get token
$token = (Invoke-RestMethod -Uri "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" `
    -Method Post -ContentType "application/x-www-form-urlencoded" `
    -Body @{grant_type="password";client_id="gateway-service";client_secret="gateway-secret";username="user1";password="password"}).access_token

# Call /api/me endpoint
Invoke-RestMethod -Uri "http://localhost:8081/api/me" `
    -Headers @{Authorization="Bearer $token"}
```

## 🔍 Health Checks

Check if services are healthy:

```powershell
# Keycloak
Invoke-WebRequest -Uri "http://localhost:8085/health"

# Tenant Service
Invoke-WebRequest -Uri "http://localhost:8083/actuator/health"

# API Service
Invoke-WebRequest -Uri "http://localhost:8084/actuator/health"

# Auth Service
Invoke-WebRequest -Uri "http://localhost:8082/actuator/health"

# Gateway Service
Invoke-WebRequest -Uri "http://localhost:8081/actuator/health"
```

## 📊 Swagger/OpenAPI

Access Swagger UI for each service:

- **Tenant Service**: http://localhost:8083/swagger-ui.html
- **API Service**: http://localhost:8084/swagger-ui.html
- **Auth Service**: http://localhost:8082/swagger-ui.html
- **Gateway Service**: http://localhost:8081/swagger-ui.html

## 🛠️ Useful Commands

### View Logs

```powershell
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f tenant-service
docker-compose logs -f keycloak
```

### Stop Services

```powershell
docker-compose down
```

### Restart Services

```powershell
docker-compose restart
```

### Rebuild and Restart

```powershell
docker-compose up -d --build
```

## 🐛 Troubleshooting

### Keycloak not ready?

Wait a bit longer (30-60 seconds) and check logs:
```powershell
docker-compose logs keycloak
```

### Service not starting?

Check logs:
```powershell
docker-compose logs [service-name]
```

### Port already in use?

Check what's using the port:
```powershell
netstat -ano | findstr :8083
```

## ✅ Verification Checklist

- [ ] Keycloak is accessible at http://localhost:8085
- [ ] Can login to Keycloak admin console
- [ ] Can get access token from Keycloak
- [ ] Tenant service responds to `/api/me` with token
- [ ] API service responds to `/api/me` with token
- [ ] Auth service responds to `/api/me` with token
- [ ] Gateway service responds to `/api/me` with token
- [ ] All services return tenant_id in response

## 🎯 Expected Response Format

When calling `/api/me` with a valid token, you should get:

```json
{
  "subject": "user-id",
  "username": "user1",
  "email": "user1@tenant1.com",
  "tenantId": "tenant1",
  "tenantIdFromJwt": "tenant1",
  "message": "This is tenant-specific data for tenant: tenant1",
  "service": "tenant-service"
}
```

---

**All services are ready to use!** 🎉


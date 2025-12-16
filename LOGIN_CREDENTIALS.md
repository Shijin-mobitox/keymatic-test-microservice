# 🔐 Login Credentials Reference

## Tenant: nwind123

### ✅ CORRECT Credentials
```
Email: admin@admin.com
Password: admin
Tenant ID: nwind123
```

### Alternative Admin Account
```
Email: admin@kymatic.com
Password: admin
Tenant ID: nwind123
```

## Working cURL Command

```bash
curl "http://localhost:8083/api/auth/login" ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-ID: nwind123" ^
  -H "Origin: http://nwind123.localhost:5173" ^
  --data-raw "{\"email\":\"admin@admin.com\",\"password\":\"admin\",\"tenantId\":\"nwind123\"}"
```

## PowerShell Test

```powershell
$body = '{"email":"admin@admin.com","password":"admin","tenantId":"nwind123"}'
Invoke-WebRequest -Uri "http://localhost:8083/api/auth/login" `
  -Method Post `
  -Body $body `
  -ContentType "application/json" `
  -Headers @{"X-Tenant-ID"="nwind123"}
```

## Expected Response (200 OK)

```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": "f80ecb29-aa2f-4967-bf63-c74727b8b38e",
  "email": "admin@admin.com",
  "tenantId": "5420fd01-f7d2-4a63-abd9-9885b0035e49",
  "role": "admin"
}
```

## All Available Tenants

### 1. nwind123 (Northwind 123)
- **Email**: `admin@admin.com` or `admin@kymatic.com`
- **Password**: `admin`
- **URL**: `http://nwind123.localhost:5173`
- **Database**: `nwind123`

### 2. tenant1
- **Email**: `admin@tenant1.com`
- **Password**: `adming`
- **URL**: `http://tenant1.localhost:5173`
- **Database**: `tenant1`

## Common Errors

### 401 Unauthorized
**Causes**:
1. Wrong email (e.g., `admin@kymatic.com` instead of `admin@admin.com`)
2. Wrong password (e.g., `admin` instead of `adming`)
3. User is inactive
4. Tenant doesn't exist

**Solution**: Use the correct credentials listed above

### 400 Bad Request
**Causes**:
1. Tenant ID not found
2. Missing required fields
3. Invalid JSON format

**Solution**: Verify tenant exists and JSON is valid

### 500 Internal Server Error
**Causes**:
1. Database connection issue
2. Missing tenant database
3. Backend service error

**Solution**: Check backend logs with `docker compose logs tenant-service`

## How to Create New Users

After logging in, you can create additional users via the UI or API:

```bash
POST /api/tenant-users
Headers:
  Authorization: Bearer {token}
  Content-Type: application/json
Body:
{
  "email": "newuser@example.com",
  "password": "password123",
  "tenantId": "5420fd01-f7d2-4a63-abd9-9885b0035e49",
  "role": "user"
}
```

## Password Information

### nwind123 Tenant
Both admin accounts use password: `admin`

### Other Tenants
Tenant admin users created via the provisioning service have the default password: `adming`

**⚠️ IMPORTANT**: Change default passwords in production!

---

**Quick Copy-Paste for nwind123**:
```
Email: admin@admin.com
Password: admin
```

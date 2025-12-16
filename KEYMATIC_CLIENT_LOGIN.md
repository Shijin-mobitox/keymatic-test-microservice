# ✅ Keymatic Client Login - WORKING

## Tenant: nwind123

### Access URL
```
http://nwind123.localhost:5173
```

### Login Credentials
```
Email: admin@admin.com
Password: adming
```

**Note**: The password is `adming` (with a 'g'), not `admin`

### Tenant Details
- **Tenant ID**: `5420fd01-f7d2-4a63-abd9-9885b0035e49`
- **Tenant Name**: `Northwind 123`
- **Slug**: `nwind123`
- **Database**: `nwind123` (15 tables created)
- **Status**: Active
- **Subscription**: Premium

### Database Tables
✅ All tables created successfully:
- `users` - User profiles
- `projects` - Projects
- `tasks` - Tasks
- `activity_log` - Activity logs
- `roles`, `permissions`, `role_permissions` - RBAC
- `sites`, `user_site_access` - Site access control
- `departments`, `user_departments` - Department management
- `teams`, `team_members` - Team management
- `flyway_schema_history` - Migration tracking

### Verified Working
✅ Login endpoint: `POST /api/auth/login`
✅ Tenant database: `nwind123` exists with all tables
✅ Admin user: `admin@admin.com` (role: admin, active)
✅ Master database routing: Working
✅ Tenant database routing: Working

### Test Login via cURL
```powershell
$body = '{"email":"admin@admin.com","password":"adming","tenantId":"nwind123"}'
Invoke-WebRequest -Uri "http://localhost:8083/api/auth/login" `
  -Method Post `
  -Body $body `
  -ContentType "application/json" `
  -Headers @{"X-Tenant-ID"="nwind123"}
```

### Expected Response
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

## Other Available Tenants

### tenant1
- **URL**: `http://tenant1.localhost:5173`
- **Email**: `admin@tenant1.com`
- **Password**: `adming`
- **Database**: `tenant1`

## Troubleshooting

### 400 Bad Request
- **Cause**: Wrong password or tenant doesn't exist
- **Solution**: Use password `adming` (not `admin`)

### 401 Unauthorized
- **Cause**: User inactive or credentials invalid
- **Solution**: Check user is active in `tenant_users` table

### 500 Internal Server Error
- **Cause**: Database connection issue or missing tables
- **Solution**: Check tenant database exists and has tables

---

**Status**: ✅ FULLY OPERATIONAL  
**Last Verified**: November 27, 2025


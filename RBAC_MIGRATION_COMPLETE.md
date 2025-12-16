# ✅ RBAC Migration Complete - Users Table Fixed

## Problem
The `UserEntity` had a `role` column mapped, but the actual tenant database schema doesn't have this column. Roles are managed through the RBAC system (`user_roles`, `roles`, `permissions` tables), not as a direct column on the `users` table.

## Error
```
JDBC exception executing SQL [...] 
ERROR: column ue1_0.role does not exist
```

## Solution
Removed the `role` field from:
1. **UserEntity** - Commented out `role` property and its getters/setters
2. **UserService** - Removed `setRole()` calls, return `null` for role in responses
3. **UserRepository** - Commented out `findByRole(String)` method

## Files Changed
- `tenant-service/src/main/java/com/kymatic/tenantservice/persistence/entity/tenant/UserEntity.java`
- `tenant-service/src/main/java/com/kymatic/tenantservice/service/tenant/UserService.java`
- `tenant-service/src/main/java/com/kymatic/tenantservice/persistence/repository/tenant/UserRepository.java`

## How Roles Work Now

### Master Database (`postgres`)
**Table**: `tenant_users`  
**Purpose**: Authentication (login credentials)  
**Has `role` column**: ✅ YES - Simple role for authentication (admin, user, etc.)

### Tenant Databases (e.g., `nwind123`)
**Table**: `users`  
**Purpose**: User profiles and information  
**Has `role` column**: ❌ NO - Roles managed through RBAC system

**RBAC Tables**:
- `roles` - Role definitions (id, name, description)
- `permissions` - Permission definitions (id, name, resource, action)
- `role_permissions` - Maps roles to permissions
- `user_roles` - Assigns roles to users
- `sites` - Site/location definitions
- `user_site_access` - Grants users access to specific sites

## API Response Format

### `/api/users` Response
```json
[{
  "userId": "f80ecb29-aa2f-4967-bf63-c74727b8b38e",
  "email": "admin@admin.com",
  "firstName": "Admin",
  "lastName": "User",
  "role": null,  ← Always null (use RBAC endpoints instead)
  "isActive": true,
  "createdAt": "2025-11-27T10:43:22.595688Z",
  "updatedAt": "2025-11-27T10:43:22.595688Z"
}]
```

### To Get User Roles
Use the RBAC endpoints:
```
GET /api/rbac/users/{userId}/roles
GET /api/rbac/users/{userId}/permissions
```

## Verified Working

### ✅ Login
```bash
POST http://localhost:8083/api/auth/login
Body: {"email":"admin@admin.com","password":"admin","tenantId":"nwind123"}
Response: 200 OK with JWT token
```

### ✅ Get Users
```bash
GET http://localhost:8083/api/users
Headers: 
  Authorization: Bearer {token}
  X-Tenant-ID: 5420fd01-f7d2-4a63-abd9-9885b0035e49
Response: 200 OK with user list
```

### ✅ Get Projects
```bash
GET http://localhost:8083/api/projects
Headers: 
  Authorization: Bearer {token}
  X-Tenant-ID: 5420fd01-f7d2-4a63-abd9-9885b0035e49
Response: 200 OK with project list
```

### ✅ Get Tasks
```bash
GET http://localhost:8083/api/tasks
Headers: 
  Authorization: Bearer {token}
  X-Tenant-ID: 5420fd01-f7d2-4a63-abd9-9885b0035e49
Response: 200 OK with task list
```

## Database Schema

### Tenant Database `users` Table
```sql
Column         | Type                        | Notes
---------------|-----------------------------|-----------------------
user_id        | uuid                        | Primary key
tenant_id      | character varying(255)      | Tenant identifier
email          | character varying(255)      | Unique, not null
first_name     | character varying(100)      |
last_name      | character varying(100)      |
phone          | character varying(50)       |
is_active      | boolean                     | Default: true
email_verified | boolean                     | Default: false
last_login     | timestamp without time zone |
created_at     | timestamp without time zone | Auto-generated
updated_at     | timestamp without time zone | Auto-updated
deleted_at     | timestamp without time zone | Soft delete
```

**Note**: No `role` column - use `user_roles` table instead

### RBAC Tables
```sql
-- Roles
roles (role_id, name, description, created_at)

-- Permissions
permissions (permission_id, name, description, resource, action, created_at)

-- Role-Permission Mapping
role_permissions (role_id, permission_id, granted_at)

-- User-Role Assignment
user_roles (user_id, role_id, assigned_by, assigned_at)

-- Site Access
sites (site_id, name, location, ...)
user_site_access (user_id, site_id, granted_by, granted_at)
```

## Migration Notes

### For Existing Code
If you have code that expects `user.role` from the API:
1. Update to use `null` or remove the field
2. Use RBAC endpoints to get user roles: `/api/rbac/users/{userId}/roles`
3. Check permissions via: `/api/rbac/users/{userId}/permissions`

### For New Features
- Don't add `role` column to `users` table
- Use RBAC system for all authorization
- Assign roles via `/api/rbac/users/{userId}/roles` endpoint
- Check permissions via RBAC service

## Status

✅ **Backend**: All endpoints operational  
✅ **Database**: Schema matches entity definitions  
✅ **RBAC**: Full role-based access control system available  
✅ **Authentication**: Login working with master DB `tenant_users`  
✅ **Authorization**: Roles managed through tenant DB RBAC tables  

---

**Date**: November 27, 2025  
**Status**: ✅ COMPLETE AND OPERATIONAL  
**Breaking Change**: `role` field in `/api/users` response is now `null` - use RBAC endpoints instead


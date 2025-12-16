# Tenant Routing Fix Summary

## Problem
The admin panel (main UI) was getting 500 errors with message:
```
"Could not open JPA EntityManager for transaction"
```

## Root Cause
Admin/management endpoints were being routed to tenant databases when they should always use the master database. The `JwtTenantResolver` was setting tenant ID in `TenantContext` for all authenticated requests, causing the routing datasource to try to connect to tenant-specific databases even for admin operations.

## Solution
Modified `JwtTenantResolver` to bypass tenant routing for admin/management endpoints by:
1. **Added master DB only patterns** - These endpoints never set tenant ID in `TenantContext`
2. **Early exit for admin endpoints** - Admin endpoints skip tenant resolution entirely
3. **Master DB fallback** - When no tenant ID is set, routing datasource uses master database

## Endpoint Categorization

### Master Database Endpoints (Admin Panel)
These endpoints **always use master database** and bypass tenant routing:
- `/api/tenants` - Tenant management (create, list, get tenant info)
- `/api/tenant-users` - Tenant user management in master DB (`tenant_users` table)
- `/api/subscriptions` - Subscription management
- `/api/audit-logs` - Audit log viewing (admin querying tenant logs)
- `/actuator` - Spring Boot actuator endpoints (health, info, etc.)

### Tenant Database Endpoints (Keymatic Client)
These endpoints **use dynamic tenant routing** based on `X-Tenant-ID` header:
- `/api/users` - Tenant-specific users (`users` table in tenant DB)
- `/api/projects` - Tenant-specific projects
- `/api/tasks` - Tenant-specific tasks
- `/api/activity-logs` - Tenant-specific activity logs
- `/api/rbac` - Role-based access control (tenant-specific RBAC)

## Implementation Details

### JwtTenantResolver Changes
- Added `MASTER_DB_ONLY_PATTERNS` array with admin endpoint patterns
- Added `isMasterDbOnlyEndpoint()` method to check if request should use master DB
- Early return for admin endpoints - they skip tenant ID extraction and setting

### How It Works
1. **Admin Panel Request** (e.g., `/api/tenants`):
   - `JwtTenantResolver` detects admin endpoint pattern
   - Skips tenant ID extraction
   - Does NOT set tenant ID in `TenantContext`
   - `TenantRoutingDataSource.determineCurrentLookupKey()` returns `MASTER_DATABASE_KEY`
   - Request uses master database

2. **Keymatic Client Request** (e.g., `/api/users` with `X-Tenant-ID: testcompany`):
   - `JwtTenantResolver` extracts tenant ID from header or JWT
   - Sets tenant ID in `TenantContext`
   - `TenantRoutingDataSource` resolves tenant ID → database name (e.g., `testcompany`)
   - Request uses tenant-specific database

## Database Structure

### Master Database (`postgres`)
- `tenants` table - Tenant metadata
- `tenant_users` table - Authentication credentials
- `tenant_migrations` table - Migration tracking
- `subscriptions` table - Subscription data
- `audit_logs` table - System-wide audit logs

### Tenant Databases (e.g., `testcompany`)
- `users` table - Tenant-specific user profiles
- `projects` table - Tenant projects
- `tasks` table - Tenant tasks
- `activity_logs` table - Tenant activity logs
- `rbac_*` tables - Tenant RBAC (roles, permissions, sites, assignments)

## Testing Checklist

- [x] Admin panel can access `/api/tenants` without errors
- [x] Admin panel can access `/api/tenant-users` without errors
- [x] Admin panel can access `/api/subscriptions` without errors
- [x] Admin panel can access `/api/audit-logs` without errors
- [x] Keymatic client can access tenant-specific endpoints with `X-Tenant-ID` header
- [x] Tenant routing works correctly for dynamic tenant databases
- [x] Master database operations work without tenant ID

## Files Modified

1. **tenant-service/src/main/java/com/kymatic/tenantservice/multitenancy/JwtTenantResolver.java**
   - Added `MASTER_DB_ONLY_PATTERNS` constant
   - Added `isMasterDbOnlyEndpoint()` method
   - Modified `doFilterInternal()` to skip tenant routing for admin endpoints

## Deployment

After making these changes:
1. Rebuild tenant-service: `.\gradlew.bat :tenant-service:bootJar`
2. Rebuild Docker image: `docker compose build tenant-service`
3. Restart service: `docker compose restart tenant-service`

## Verification

To verify the fix is working:
1. Check tenant-service logs for: `"Admin/management endpoint detected: {path}. Skipping tenant routing"`
2. Admin panel should work without 500 errors
3. Keymatic client should continue working with tenant routing


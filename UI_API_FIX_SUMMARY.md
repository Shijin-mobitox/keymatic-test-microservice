# UI API Fix Summary

## Problem
The main UI (front-end) was failing with `ERR_EMPTY_RESPONSE` when calling:
- `http://localhost:8083/api/me`
- `http://localhost:8083/api/tenant/info`

## Root Cause
These endpoints exist in `TenantAwareController` but were **not** in the master-only routing bypass list. When the UI called them:

1. The request hit `JwtTenantResolver` filter
2. Since `/api/me` and `/api/tenant/info` weren't in `MASTER_DB_ONLY_PATTERNS`, the filter tried to extract tenant ID
3. With no tenant ID in context, the routing datasource tried to route to a tenant database
4. The endpoints don't actually query a database—they just read from `TenantContext` and return JWT claims
5. The mismatch caused connection issues, resulting in `ERR_EMPTY_RESPONSE`

## Solution
Added `/api/me`, `/api/tenant/current`, and `/api/tenant/info` to the master-only bypass patterns in both filters:

### Files Updated

1. **`tenant-service/src/main/java/com/kymatic/tenantservice/multitenancy/JwtTenantResolver.java`**
   - Added `/api/me`, `/api/tenant/current`, `/api/tenant/info` to `MASTER_DB_ONLY_PATTERNS`

2. **`tenant-service/src/main/java/com/kymatic/tenantservice/config/TenantFilter.java`**
   - Added `/api/me`, `/api/tenant/current`, `/api/tenant/info` to `MASTER_DB_ONLY_PATTERNS`

3. **Frontend TypeScript Fixes**
   - `front-end/src/App.tsx` - Fixed `token` type from `string | undefined` to `string`
   - `front-end/src/components/admin/CreateTenantForm.tsx` - Fixed error prop type
   - `front-end/src/components/CreateTenantUserForm.tsx` - Fixed error prop type
   - `front-end/src/services/tenantService.ts` - Added `databaseName` to `TenantInfo` interface
   - `keymatic-client/vite.config.ts` - Fixed proxy error handler types

## Updated Master-Only Endpoint List

These endpoints now bypass tenant routing and use the master database:

```java
private static final String[] MASTER_DB_ONLY_PATTERNS = {
    "/api/tenants",           // Tenant management (create, list, get tenant)
    "/api/tenant-users",      // Tenant user management in master DB
    "/api/subscriptions",     // Subscription management
    "/api/audit-logs",        // Audit log viewing (admin querying tenant logs)
    "/api/me",                // User info endpoint (reads from JWT, no DB query)
    "/api/tenant/current",    // Current tenant endpoint (reads from context)
    "/api/tenant/info",       // Tenant info endpoint (reads from context)
    "/actuator"               // Spring Boot actuator endpoints
};
```

## How It Works Now

### Before (Broken)
```
Browser → /api/me (with JWT)
    ↓
JwtTenantResolver: Not in bypass list, try to route to tenant DB
    ↓
No tenant DB routing configured for info endpoints
    ↓
Connection closes → ERR_EMPTY_RESPONSE
```

### After (Fixed)
```
Browser → /api/me (with JWT)
    ↓
JwtTenantResolver: Detected master-only endpoint, skip tenant routing
    ↓
TenantContext cleared (no tenant ID set)
    ↓
Controller reads JWT claims and returns data
    ↓
✅ Success
```

## Deployment Steps

1. **Rebuild tenant-service:**
   ```bash
   .\gradlew.bat :tenant-service:build -x test
   ```

2. **Rebuild Docker image (if using Docker):**
   ```bash
   docker compose build tenant-service
   ```

3. **Restart tenant-service:**
   ```bash
   # If running via Gradle:
   .\gradlew.bat :tenant-service:bootRun
   
   # If running via Docker:
   docker compose restart tenant-service
   ```

4. **Verify frontend builds:**
   ```bash
   cd front-end
   npm run build
   
   cd ../keymatic-client
   npm run build
   ```

## Testing

After restarting tenant-service, test these endpoints:

```bash
# Get JWT token from Keycloak first
TOKEN="your-jwt-token-here"

# Test /api/me
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/me

# Test /api/tenant/info
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/tenant/info

# Test /api/tenant/current
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/tenant/current
```

All should return 200 OK with JSON data.

## Status
✅ **Backend build successful**
✅ **Frontend builds successful**
✅ **Routing patterns updated**
⏳ **Awaiting service restart and verification**

## Next Steps
1. Restart tenant-service with the updated build
2. Reload the UI in browser
3. Verify `/api/me` and `/api/tenant/info` return data instead of `ERR_EMPTY_RESPONSE`
4. If still failing, check tenant-service logs for any exceptions during the request


# Dynamic Database Routing by Tenant Name/Slug

## Overview

The database **automatically changes** based on the tenant name/slug, just like the URL changes. Each tenant has its own dedicated database that is dynamically selected based on the tenant identifier.

## How It Works

### Client Side

1. User accesses: `http://testcompany.localhost:5173`
2. Client extracts tenant slug: `testcompany`
3. Client sends `X-Tenant-ID: testcompany` header in all API requests
4. Client constructs API URL: `http://testcompany.localhost:8083`

### Backend Side

1. **Tenant ID Extraction**: `JwtTenantResolver` extracts `testcompany` from `X-Tenant-ID` header
2. **Tenant Context**: Stores `testcompany` in `TenantContext` (ThreadLocal)
3. **Database Resolution**: `TenantDatabaseResolver` resolves `testcompany` → database name
4. **Database Routing**: All queries automatically route to tenant's database

## Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│  Client: http://testcompany.localhost:5173                  │
│  → Tenant Slug: "testcompany"                               │
│  → API Header: X-Tenant-ID: testcompany                     │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ HTTP Request
                       │ X-Tenant-ID: testcompany
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  Backend: tenant-service                                     │
│  → Extracts: "testcompany" from header                      │
│  → Resolves: testcompany → tenant_testcompany_db            │
│  → Routes queries to: tenant_testcompany_db                 │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Database Queries
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  Tenant Database: tenant_testcompany_db                     │
│  → Tables: users, projects, tasks, etc.                     │
│  → All data for "testcompany" tenant                        │
└─────────────────────────────────────────────────────────────┘
```

## Examples

### Example 1: testcompany Tenant

**URL**: `http://testcompany.localhost:5173`  
**Tenant Slug**: `testcompany`  
**Database**: `tenant_testcompany_db` (or based on tenant name)  
**Email**: `admin@testcompany.local`  
**Password**: `adming`

**What happens:**
1. Client detects tenant: `testcompany`
2. Sends header: `X-Tenant-ID: testcompany`
3. Backend routes to: `testcompany` tenant's database
4. All queries go to that tenant's isolated database

### Example 2: mycompany Tenant

**URL**: `http://mycompany.localhost:5173`  
**Tenant Slug**: `mycompany`  
**Database**: `tenant_mycompany_db` (different database!)  
**Email**: `admin@mycompany.local`  
**Password**: `adming`

**What happens:**
1. Client detects tenant: `mycompany`
2. Sends header: `X-Tenant-ID: mycompany`
3. Backend routes to: `mycompany` tenant's database
4. All queries go to that tenant's isolated database

## Key Points

✅ **One tenant = One database**: Each tenant has a completely separate database  
✅ **Automatic routing**: Database is selected automatically based on tenant slug  
✅ **URL-based**: Database changes just like URL changes (`testcompany.localhost` → `testcompany` database)  
✅ **Data isolation**: Each tenant's data is completely isolated  
✅ **No manual switching**: Backend automatically routes based on `X-Tenant-ID` header

## Database Naming

The database name is created based on the tenant name during tenant provisioning:

```java
String databaseName = buildDatabaseName(request.tenantName());
```

For example:
- Tenant Name: "Test Company" → Database: `tenant_test_company_db`
- Tenant Slug: "testcompany" → Database: `tenant_testcompany_db`

The exact naming convention depends on the `buildDatabaseName()` method implementation.

## Client Configuration

The client automatically includes the tenant ID in all requests:

**File**: `keymatic-client/src/services/api.ts`

```typescript
this.api.interceptors.request.use((config) => {
  const tenantId = this.getTenantId(); // Gets tenant slug (e.g., "testcompany")
  if (tenantId) {
    config.headers['X-Tenant-ID'] = tenantId; // ✅ Automatically included!
  }
  return config;
});
```

## Backend Resolution

The backend resolves tenant slug to database name:

**File**: `tenant-service/src/main/java/com/kymatic/tenantservice/service/TenantDatabaseResolver.java`

```java
public String getCurrentTenantDatabaseName() {
    String tenantId = TenantContext.getTenantId(); // Gets "testcompany"
    TenantEntity tenant = tenantIdentifierResolver.resolveTenantEntity(tenantId);
    return tenant.getDatabaseName(); // Returns "tenant_testcompany_db"
}
```

## Summary

✅ **Database changes dynamically** based on tenant name/slug  
✅ **Just like URL** - `testcompany.localhost` → `testcompany` database  
✅ **Automatic routing** - No manual database selection needed  
✅ **Complete isolation** - Each tenant's data is in a separate database


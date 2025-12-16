# Database Naming by Tenant Slug

## Overview

The database name is **automatically created from the tenant slug** when a tenant is provisioned. This ensures the database name matches the URL pattern.

## Database Naming Rule

**Database Name = Tenant Slug**

### Examples

| Tenant Slug | URL | Database Name |
|-------------|-----|---------------|
| `testcompany` | `http://testcompany.localhost:5173` | `testcompany` |
| `mycompany` | `http://mycompany.localhost:5173` | `mycompany` |
| `shijintest123` | `http://shijintest123.localhost:5173` | `shijintest123` |

## How It Works

### During Tenant Creation

1. **Tenant Request**:
   ```json
   {
     "tenantName": "Test Company",
     "slug": "testcompany"
   }
   ```

2. **Database Creation**:
   - Database name: `testcompany` (from slug)
   - Database created: `CREATE DATABASE "testcompany"`
   - Migrations run on: `testcompany` database

3. **Result**:
   - Tenant slug: `testcompany`
   - Database name: `testcompany`
   - Perfect match! ✅

### During Runtime

1. **Client Access**: `http://testcompany.localhost:5173`
   - Client extracts slug: `testcompany`
   - Sends header: `X-Tenant-ID: testcompany`

2. **Backend Routing**:
   - Extracts tenant slug: `testcompany`
   - Resolves to database: `testcompany`
   - Routes queries to: `testcompany` database

3. **Result**:
   - All queries go to `testcompany` database
   - Complete data isolation

## Benefits

✅ **Consistency**: Database name matches slug and URL  
✅ **Predictability**: Easy to know which database belongs to which tenant  
✅ **Simplicity**: No complex naming schemes  
✅ **Dynamic Routing**: Automatic database selection based on slug

## Implementation

**File**: `tenant-service/src/main/java/com/kymatic/tenantservice/service/TenantProvisioningService.java`

```java
private String buildDatabaseNameFromSlug(String slug) {
    // Database name = slug (sanitized for PostgreSQL)
    // Example: "testcompany" → "testcompany"
    return sanitizedSlug;
}
```

The database name is built directly from the slug, ensuring it matches the tenant identifier used in URLs and headers.

## Summary

- ✅ **Database name = Tenant slug**
- ✅ **URL pattern**: `{slug}.localhost` → Database: `{slug}`
- ✅ **Automatic**: Created during tenant provisioning
- ✅ **Dynamic**: Routing based on slug


# Client vs Backend - Database Routing Responsibilities

## Clarification: Where Does Database Routing Happen?

### ✅ Client App (keymatic-client) - **Sends Tenant Identifier**

The client app's role is to **send the tenant identifier** in API requests:

1. **Detects tenant from URL**: `http://testcompany.localhost:5173` → extracts `testcompany`
2. **Sends in header**: `X-Tenant-ID: testcompany`
3. **That's it!** The client doesn't know or care about databases

**Files:**
- `keymatic-client/src/utils/tenant.ts` - Extracts tenant from hostname
- `keymatic-client/src/services/api.ts` - Adds `X-Tenant-ID` header to requests
- `keymatic-client/src/config/api.ts` - Constructs API URL

**What Client Does:**
- ✅ Detects tenant from hostname
- ✅ Sends `X-Tenant-ID` header
- ❌ **Does NOT** connect to databases
- ❌ **Does NOT** do database routing

---

### ✅ Backend (tenant-service) - **Routes to Tenant Database**

The backend is responsible for **routing database queries** to the correct tenant database:

1. **Receives header**: `X-Tenant-ID: testcompany`
2. **Extracts tenant**: Stores in `TenantContext`
3. **Resolves database**: `testcompany` → database name `testcompany`
4. **Routes queries**: All JPA queries go to `testcompany` database

**Files:**
- `tenant-service/config/TenantRoutingDataSource.java` - Routes queries dynamically
- `tenant-service/config/TenantDataSourceConfig.java` - Configures routing
- `tenant-service/service/TenantDatabaseResolver.java` - Resolves tenant → database
- `tenant-service/multitenancy/JwtTenantResolver.java` - Extracts tenant from header

**What Backend Does:**
- ✅ Receives tenant ID from header
- ✅ Resolves tenant → database name
- ✅ Routes all database queries to tenant database
- ✅ Creates datasources dynamically
- ✅ **This is where database routing happens!**

---

## Complete Flow

```
┌─────────────────────────────────────────────────────────────┐
│  CLIENT APP (keymatic-client)                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. User accesses: testcompany.localhost:5173              │
│  2. Extracts tenant slug: "testcompany"                    │
│  3. Sends header: X-Tenant-ID: testcompany                 │
│                                                             │
│  ❌ Client does NOT connect to databases                    │
│  ❌ Client does NOT do database routing                     │
│                                                             │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ HTTP Request
                       │ X-Tenant-ID: testcompany
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  BACKEND (tenant-service)                                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Receives header: X-Tenant-ID: testcompany              │
│  2. Extracts tenant → stores in TenantContext              │
│  3. Resolves: testcompany → database name: "testcompany"   │
│  4. Routes ALL database queries to "testcompany" database  │
│                                                             │
│  ✅ Backend does database routing!                          │
│  ✅ All JPA repositories use tenant database automatically │
│                                                             │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Database Query
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  TENANT DATABASE: testcompany                                │
│  → All queries routed here automatically                    │
└─────────────────────────────────────────────────────────────┘
```

---

## Summary

### Client App (keymatic-client)
- **Role**: Send tenant identifier
- **Sends**: `X-Tenant-ID` header with tenant slug
- **Does NOT**: Connect to databases or do routing

### Backend (tenant-service)
- **Role**: Route database queries to tenant databases
- **Does**: 
  - Receives tenant ID
  - Resolves to database name
  - Routes queries dynamically
  - Creates datasources on-demand

---

## Answer to Your Question

**"This is only for the client app right?"**

**Answer**: No! The **database routing happens in the BACKEND**, not the client.

- **Client app**: Only sends the tenant identifier (`X-Tenant-ID` header)
- **Backend**: Receives it and routes database queries to the tenant's database

The dynamic database routing is a **BACKEND feature**. The client just tells the backend which tenant it is by sending the tenant slug in the header.

---

## Key Point

✅ **Database routing is a BACKEND responsibility**  
✅ **Client only sends tenant identifier**  
✅ **Backend routes queries to tenant databases automatically**


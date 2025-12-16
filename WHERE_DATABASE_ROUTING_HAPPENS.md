# Where Database Routing Happens - Clarification

## Quick Answer

**No!** Database routing is **NOT only for the client app**. 

- ✅ **Client app**: Only sends tenant identifier (header)
- ✅ **Backend**: Does the actual database routing

---

## Breakdown

### Client App (keymatic-client)

**Role**: Frontend React application
- ✅ Detects tenant from URL
- ✅ Sends `X-Tenant-ID` header
- ❌ **Does NOT connect to databases**
- ❌ **Does NOT do database routing**

**What it does:**
```typescript
// Client only sends header
config.headers['X-Tenant-ID'] = tenantId; // "testcompany"
```

---

### Backend (tenant-service)

**Role**: Backend Spring Boot service
- ✅ Receives tenant ID from header
- ✅ Resolves tenant → database name
- ✅ Routes database queries dynamically
- ✅ **This is where database routing happens!**

**What it does:**
```java
// Backend routes queries to tenant database
TenantRoutingDataSource → routes to "testcompany" database
```

---

## Complete Flow

```
CLIENT (keymatic-client)
  ↓
Detects: testcompany.localhost:5173
  ↓
Sends: X-Tenant-ID: testcompany
  ↓
─────────────────────────────────
  ↓
BACKEND (tenant-service)
  ↓
Receives header: X-Tenant-ID: testcompany
  ↓
Resolves: testcompany → database: "testcompany"
  ↓
Routes ALL queries → "testcompany" database ✅
```

---

## Summary

- **Client**: Sends tenant ID (doesn't know about databases)
- **Backend**: Receives tenant ID → routes to tenant database

**Database routing happens in the BACKEND, not the client!**


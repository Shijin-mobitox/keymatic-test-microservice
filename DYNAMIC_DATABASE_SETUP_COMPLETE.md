# ✅ Dynamic Database Setup - Complete Guide

## Current Status

### ✅ Client App (keymatic-client) - **ALREADY CONFIGURED CORRECTLY**

The client app **already sends tenant ID dynamically** in all API requests:

1. **Tenant Detection**: 
   - Auto-detects tenant from hostname (`tenant1.localhost:5173` → `tenant1`)
   - Extracts tenant slug using `getTenantFromHostname()`

2. **API URL Construction**:
   - Dynamically constructs API URL: `http://{tenant}.localhost:8083`
   - Example: `http://tenant1.localhost:8083`

3. **Tenant ID Header**:
   - Automatically includes `X-Tenant-ID` header in ALL API requests
   - Value: Tenant slug (e.g., `tenant1`)

**Code Location**: `keymatic-client/src/services/api.ts`
```typescript
this.api.interceptors.request.use((config) => {
  const tenantId = this.getTenantId(); // Gets from localStorage or TENANT_SLUG
  if (tenantId) {
    config.headers['X-Tenant-ID'] = tenantId; // ✅ Already sending!
  }
  return config;
});
```

### ✅ Backend (tenant-service) - **Partially Configured**

1. **Tenant ID Extraction**: ✅ Working
   - `JwtTenantResolver` extracts tenant ID from `X-Tenant-ID` header
   - Stores in `TenantContext` (ThreadLocal)

2. **Tenant Database Resolution**: ✅ Implemented
   - `TenantDatabaseResolver` service created
   - Resolves tenant ID → database name

3. **Database Routing**: ⚠️ **NEEDS IMPLEMENTATION**
   - Repositories still use default datasource (master DB)
   - Need to route queries to tenant-specific databases

## How It Works

### Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT APP (keymatic-client)             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. User accesses: tenant1.localhost:5173                  │
│  2. Tenant detected: "tenant1"                             │
│  3. API URL: http://tenant1.localhost:8083                 │
│  4. ALL API requests include header:                        │
│     X-Tenant-ID: tenant1                                    │
│                                                             │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ HTTP Request with X-Tenant-ID header
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                 BACKEND (tenant-service)                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. JwtTenantResolver extracts tenant ID from header       │
│  2. Stores in TenantContext: "tenant1"                     │
│  3. TenantDatabaseResolver resolves:                       │
│     tenant1 → database_name (e.g., "tenant_1_db")          │
│  4. Queries route to tenant-specific database              │
│                                                             │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Database Query
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              TENANT-SPECIFIC DATABASE                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Database: tenant_1_db                                      │
│  Tables: users, projects, tasks, etc.                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## ✅ What's Already Working

### Client Side
- ✅ Tenant detection from hostname
- ✅ Dynamic API URL construction  
- ✅ Automatic `X-Tenant-ID` header injection
- ✅ Token-based authentication
- ✅ All API endpoints integrated

### Backend Side
- ✅ Tenant ID extraction from headers
- ✅ TenantContext storage
- ✅ Tenant database name resolution
- ✅ Tenant database creation during provisioning
- ✅ Tenant-specific database migrations

## ⚠️ What Needs Implementation

The backend repositories need to **dynamically route to tenant databases**:

**Current Issue**: Repositories use default datasource (master DB)
**Solution Needed**: Implement routing datasource or update services to use tenant databases

### Options:

**Option 1: Multi-Tenant Datasource Router** (Recommended for JPA)
- Create `AbstractRoutingDataSource` implementation
- Routes to tenant database based on `TenantContext`
- Transparent to repositories

**Option 2: Service-Level Database Switching**
- Update services to use `TenantDatabaseResolver`
- Use `TenantDatabaseManager` for JDBC queries
- More explicit but requires service changes

## Summary

**✅ Client App**: Fully configured - dynamically sends tenant ID in headers  
**✅ Backend Setup**: Tenant ID extraction and database resolution working  
**⚠️ Database Routing**: Needs implementation to route queries to tenant databases

The client app is **already doing everything correctly**. The remaining work is in the backend to ensure repositories route to tenant-specific databases based on the tenant ID from `TenantContext`.


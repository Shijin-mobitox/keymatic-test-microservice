# Dynamic Database Routing for Multi-Tenancy

## Problem

Currently, the backend services use the default datasource (master database) for all tenant-specific repositories. We need to route queries to tenant-specific databases dynamically based on the tenant ID from `TenantContext`.

## Solution: Tenant-Aware Datasource Router

Create a custom `AbstractRoutingDataSource` that:
1. Gets tenant ID from `TenantContext`
2. Looks up tenant's database name from `tenants` table
3. Routes queries to the tenant-specific database

## Implementation Plan

1. Create `TenantRoutingDataSource` - Custom routing datasource
2. Create `TenantDataSourceResolver` - Resolves tenant ID to database name
3. Configure dynamic datasource routing in Spring configuration
4. Update services to use tenant-specific datasource

This will ensure that when the client app sends requests with `X-Tenant-ID` header:
- Tenant ID is extracted and stored in `TenantContext`
- All database queries automatically route to the tenant's database
- Each tenant's data is completely isolated in their own database


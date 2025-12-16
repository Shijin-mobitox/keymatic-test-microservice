# Dynamic Tenant Database Routing - Implementation Guide

## Current Issue

The client app already sends `X-Tenant-ID` header, and the backend extracts it into `TenantContext`. However, tenant-specific repositories (like `UserRepository`) are still using the master database instead of the tenant's specific database.

## Solution

We need to implement dynamic database routing so that:
1. Client sends requests with `X-Tenant-ID` header (already done ✅)
2. Backend extracts tenant ID to `TenantContext` (already done ✅)
3. Backend resolves tenant ID to database name (need to implement)
4. Backend routes queries to tenant-specific database (need to implement)

## Implementation Steps

1. Create `TenantDatabaseResolver` service that:
   - Takes tenant ID (UUID or slug) from `TenantContext`
   - Looks up tenant's database name from `tenants` table
   - Returns database name

2. Update tenant-specific services to:
   - Get tenant ID from `TenantContext`
   - Resolve database name using `TenantDatabaseResolver`
   - Use `TenantDatabaseManager` to execute queries in tenant database

3. Or implement a routing datasource that automatically switches databases based on tenant ID.


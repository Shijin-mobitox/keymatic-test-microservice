# Database Structure Guide

## Overview

The system uses a **multi-database architecture** with:
- **1 Master Database** - Stores tenant metadata and authentication
- **N Tenant Databases** - One per tenant, stores tenant-specific data

## Master Database

**Location**: Shared PostgreSQL database (`postgres`)

**Tables**:
- `tenants` - Tenant metadata and configuration
- `tenant_users` - **Authentication credentials** (email, password hash, role)
- `tenant_migrations` - Migration tracking
- `tenant_audit_log` - Audit logs
- `subscriptions` - Subscription information

### Authentication Users (`tenant_users`)

**Important:** Login authentication uses this table!

When you create a tenant, the default admin user is created here:

```sql
-- Master Database
tenant_users table:
- user_id (UUID)
- tenant_id (UUID) -> references tenants table
- email (VARCHAR)
- password_hash (TEXT) - BCrypt hashed
- role (VARCHAR)
- is_active (BOOLEAN)
- last_login (TIMESTAMP)
- created_at (TIMESTAMP)
```

**Example:**
```sql
SELECT * FROM tenant_users WHERE tenant_id = 'your-tenant-uuid';
```

## Tenant Database

**Location**: Separate database per tenant (e.g., `tenant_db_mycompany`)

**Tables** (from `V1__tenant_schema.sql`):
- `users` - **User profiles** (first_name, last_name, phone, etc.)
- `sites` - Physical locations
- `projects` - Projects
- `tasks` - Tasks
- `roles` - RBAC roles
- `permissions` - RBAC permissions
- `user_site_roles` - User-site-role assignments
- `activity_log` - Activity logging

### User Profiles (`users`)

This table stores tenant-specific user profile data (different from authentication):

```sql
-- Tenant Database
users table:
- user_id (UUID)
- tenant_id (VARCHAR)
- email (VARCHAR)
- first_name (VARCHAR)
- last_name (VARCHAR)
- phone (VARCHAR)
- role (VARCHAR)
- is_active (BOOLEAN)
- email_verified (BOOLEAN)
- last_login (TIMESTAMP)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
- deleted_at (TIMESTAMP)
```

## Two User Tables - Why?

### `tenant_users` (Master DB)
- **Purpose**: Authentication & Authorization
- **Used by**: Login endpoint (`/api/auth/login`)
- **Contains**: Password hashes, login credentials
- **Scope**: All tenants in one place

### `users` (Tenant DB)
- **Purpose**: User profiles and tenant-specific data
- **Used by**: Tenant-specific operations
- **Contains**: Profile info, preferences, tenant context
- **Scope**: Isolated per tenant

## Authentication Flow

1. User logs in with email/password
2. Login endpoint (`/api/auth/login`) queries `tenant_users` in **master database**
3. Validates password against `password_hash` in `tenant_users`
4. Returns JWT token with `tenant_id` claim
5. All subsequent requests include `tenant_id` to route to correct tenant database

## Example: Creating a Tenant

When you create a tenant:

1. **Master DB**: Creates record in `tenants` table
2. **Master DB**: Creates default admin in `tenant_users` table ✅
3. **Tenant DB**: Creates new database and runs migrations (creates `users` table structure)

The default admin user is in the **master database** (`tenant_users`), ready for login immediately!

## Querying Users

### Get Authentication Users (Master DB)
```sql
-- Connect to master database
SELECT * FROM tenant_users WHERE tenant_id = 'your-tenant-uuid';
```

### Get User Profiles (Tenant DB)
```sql
-- Connect to tenant-specific database
SELECT * FROM users WHERE tenant_id = 'your-tenant-slug';
```

## Summary

| Item | Master DB | Tenant DB |
|------|-----------|-----------|
| **User Authentication** | ✅ `tenant_users` | ❌ Not used |
| **User Profiles** | ❌ Not stored | ✅ `users` |
| **Default Admin** | ✅ Created here | ✅ Also created here |

**Answer:** The default admin user created during tenant provisioning is stored in **BOTH databases**:
1. ✅ **Master database** (`tenant_users` table) - for authentication/login
2. ✅ **Tenant database** (`users` table) - for tenant user profile and data


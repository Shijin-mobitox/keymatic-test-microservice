# Tenant Creation Guide

## Automatic User Creation

**YES**, when you create a tenant, it **automatically creates a default admin user** for you.

## How It Works

### Option 1: With Admin Email (Recommended)

When you provide an `adminEmail` in the tenant creation request:

```json
POST /api/tenants
{
  "tenantName": "My Company",
  "slug": "mycompany",
  "subscriptionTier": "starter",
  "maxUsers": 100,
  "maxStorageGb": 50,
  "adminEmail": "admin@mycompany.com"
}
```

**Result:**
- ✅ Tenant created
- ✅ Database created: `mycompany` (database name = slug)
- ✅ Default admin user created automatically
- **Email**: `admin@mycompany.com`
- **Password**: `adming`
- **Role**: `admin`
- **URL**: `http://mycompany.localhost:5173`
- **Database**: `mycompany`

### Option 2: Without Admin Email (Auto-Generated)

If you don't provide `adminEmail`, a default email is automatically generated:

```json
POST /api/tenants
{
  "tenantName": "My Company",
  "slug": "mycompany",
  "subscriptionTier": "starter",
  "maxUsers": 100,
  "maxStorageGb": 50
}
```

**Result:**
- ✅ Tenant created
- ✅ Default admin user created automatically
- **Email**: `admin@{slug}.local` (e.g., `admin@mycompany.local`)
- **Password**: `adming`
- **Role**: `admin`

## Default Credentials Summary

| Field | Value |
|-------|-------|
| **Email** | Provided `adminEmail` OR `admin@{slug}.local` |
| **Password** | `adming` (always) |
| **Role** | `admin` (always) |
| **Status** | Active (always) |

## Example

### Create Tenant with Custom Email

```bash
curl -X POST http://localhost:8083/api/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "Shijin Test",
    "slug": "shijintest123",
    "subscriptionTier": "starter",
    "maxUsers": 100,
    "maxStorageGb": 50,
    "adminEmail": "admin@shijin.com"
  }'
```

**Login Credentials:**
- Email: `admin@shijin.com`
- Password: `adming`
- URL: `http://shijintest123.localhost:5173`
- **Database**: `shijintest123` (database name = slug)

### Create Tenant without Email (Auto-Generated)

```bash
curl -X POST http://localhost:8083/api/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "Test Company",
    "slug": "testcompany",
    "subscriptionTier": "starter",
    "maxUsers": 100,
    "maxStorageGb": 50
  }'
```

**Login Credentials:**
- Email: `admin@testcompany.local`
- Password: `adming`
- URL: `http://testcompany.localhost:5173`
- **Database**: `testcompany` (database name = slug)
- **Database Routing**: Automatically routes to `testcompany` database

## Database Storage

**Important:** Each tenant has its own dedicated database. The database name is **automatically created from the tenant slug**, matching the URL pattern.

### Database Structure

#### Dynamic Database Routing

Just like the URL changes based on tenant (`http://testcompany.localhost:5173`), the database **automatically changes** based on the tenant:

- **URL**: `http://testcompany.localhost:5173` → Tenant Slug: `testcompany`
- **Database Name**: `testcompany` (database name = slug)
- **Database Routing**: Automatically routes to `testcompany` database

**Key Point**: The database name matches the tenant slug exactly!

### Two Database Types

1. **Master Database** (`tenant_users` table):
   - Stores authentication credentials (email, password hash, role)
   - Used by login endpoint (`/api/auth/login`)
   - Location: Shared PostgreSQL database (`postgres`)
   - ✅ **Default admin user is created here**

2. **Tenant Database** (`users` table):
   - Stores tenant-specific user profiles and data
   - **Each tenant has its own separate database**
   - **Database name = Tenant slug** (e.g., slug: `testcompany` → database: `testcompany`)
   - Matches the URL pattern: `testcompany.localhost` → `testcompany` database
   - ✅ **Default admin user is ALSO created here**

When a tenant is created, the default admin user is automatically added to:
- ✅ `tenant_users` table in the **master database** (for authentication)
- ✅ `users` table in the **tenant-specific database** (for tenant user profile)

### How Database Routing Works

1. **Client sends request** with `X-Tenant-ID: testcompany` header
2. **Backend extracts** tenant slug from header → stores in `TenantContext`
3. **Backend resolves** tenant slug → database name (database name = slug)
4. **Backend routes** all queries to the tenant's specific database
5. **Result**: Each tenant's data is completely isolated in their own database

**Example:**
- Access `http://testcompany.localhost:5173` → Slug: `testcompany` → Database: `testcompany`
- Access `http://mycompany.localhost:5173` → Slug: `mycompany` → Database: `mycompany`

**Key Point**: The database name **matches the tenant slug exactly**, making it easy to understand the relationship:
- URL: `testcompany.localhost` → Slug: `testcompany` → Database: `testcompany`

## Important Notes

1. ✅ **Always creates a user** - A default admin user is ALWAYS created automatically
2. 🗄️ **Storage location** - User is stored in MASTER database (`tenant_users` table), NOT tenant DB
3. 🔒 **Default password** - The default password is always `adming` (change in production!)
4. 📧 **Email format** - If no email provided, uses `admin@{slug}.local`
5. ⚠️ **Duplicate email** - If a user with the same email already exists, creation is skipped (warning logged)

## After Tenant Creation

Once the tenant is created, you can immediately login:

1. Go to: `http://{slug}.localhost:5173`
2. Enter the email (from above)
3. Enter password: `adming`
4. You're in! 🎉

## Changing Password

After first login, users should change their default password for security.


# Default Login Credentials

## Local Authentication (Bypasses Keycloak)

The `keymatic-client` now uses local authentication that bypasses Keycloak. Users are stored in the `tenant_users` table in the master database.

### Default Admin User

When a new tenant is created via the tenant provisioning API, a default admin user is automatically created with:

- **Email**: The email provided in the `adminEmail` field during tenant creation
- **Password**: `adming` (default password)
- **Role**: `admin`
- **Status**: Active

### Example

For a tenant created with:
```json
{
  "tenantName": "Shijin Test",
  "slug": "shijintest123",
  "adminEmail": "admin@shijin.com"
}
```

**Login Credentials:**
- **Email**: `admin@shijin.com`
- **Password**: `adming`
- **Tenant**: `shijintest123` (from slug)

### Creating a Default User for Existing Tenants

If you need to create a default user for an existing tenant, you can:

1. **Via API** (POST to `/api/tenant-users`):
```json
{
  "tenantId": "tenant-uuid-here",
  "email": "admin@example.com",
  "password": "adming",
  "role": "admin",
  "isActive": true
}
```

2. **Via Database** (Direct SQL):
```sql
-- First, get the tenant UUID
SELECT tenant_id, tenant_name, slug FROM tenants WHERE slug = 'your-tenant-slug';

-- Then insert a user (replace tenant_id with actual UUID)
INSERT INTO tenant_users (tenant_id, email, password_hash, role, is_active)
VALUES (
  'your-tenant-uuid-here',
  'admin@example.com',
  '$2a$10$...', -- BCrypt hash for password "adming"
  'admin',
  true
);
```

### Quick Start Test Credentials

For testing, you can use these default values:

**Default Admin User:**
- **Email**: `admin@test.com`
- **Password**: `adming`
- **Role**: `admin`

**Note**: You must create a tenant first, then create the user for that tenant. The tenant slug is used to determine which tenant database to use.

### Changing Default Password

After first login, users should change their password for security. You can update a user's password via:

1. **API Endpoint**: `PUT /api/tenant-users/{userId}` with new password
2. **Direct Database Update**: Update `password_hash` column in `tenant_users` table

### Security Note

⚠️ **IMPORTANT**: Change the default password `adming` in production environments. This is only suitable for development/testing.


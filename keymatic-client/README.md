# Keymatic Client

React TypeScript client application for the KyMatic multi-tenant service.

## Features

- **Local Authentication**: Bypasses Keycloak, uses direct database authentication
- **Multi-Tenant Support**: Dynamic tenant routing based on subdomain
- **RBAC**: Role-based access control
- **Modern UI**: Built with React, TypeScript, and Vite

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Running backend services (tenant-service, etc.)

### Installation

```bash
npm install
```

### Configuration

Create a `.env.local` file in the root directory:

```env
# API Configuration
VITE_API_BASE_URL=http://localhost:8083
VITE_API_HOST_TEMPLATE=http://{tenant}.localhost:8083

# Tenant Configuration (optional, auto-detected from hostname)
VITE_TENANT_SLUG=shijintest123
```

### Running the Application

```bash
npm run dev
```

The application will be available at:
- `http://localhost:5173` (default)
- `http://{tenant}.localhost:5173` (for tenant-specific URLs)

## Default Login Credentials

### For New Tenants

When a tenant is created, a default admin user is automatically created:

- **Email**: The email provided during tenant creation
- **Password**: `adming`
- **Role**: `admin`

### Example

For tenant `shijintest123` created with email `admin@shijin.com`:

- **Email**: `admin@shijin.com`
- **Password**: `adming`
- **Tenant**: `shijintest123`

### Creating a Default User

If you need to create a default user for an existing tenant, use the PowerShell script:

```powershell
.\create-default-user.ps1 -TenantSlug "shijintest123" -Email "admin@test.com" -Password "adming"
```

Or use the API directly:

```bash
POST http://localhost:8083/api/tenant-users
Content-Type: application/json
X-Tenant-ID: shijintest123

{
  "tenantId": "tenant-uuid-here",
  "email": "admin@test.com",
  "password": "adming",
  "role": "admin",
  "isActive": true
}
```

## Authentication

The application uses **local authentication** that bypasses Keycloak:

1. Login endpoint: `POST /api/auth/login`
2. Username/email and password authentication
3. JWT token-based session
4. Token refresh: `POST /api/auth/refresh`

## Tenant Detection

The tenant is automatically detected from the hostname:

- `http://shijintest123.localhost:5173` → tenant: `shijintest123`
- `http://localhost:5173` → uses `VITE_TENANT_SLUG` or fallback

## Project Structure

```
keymatic-client/
├── src/
│   ├── auth/              # Authentication logic
│   │   ├── AuthProvider.tsx
│   │   └── localAuth.ts   # Local login (no Keycloak)
│   ├── components/        # React components
│   ├── pages/            # Page components
│   ├── services/         # API services
│   ├── config/           # Configuration
│   └── utils/            # Utilities
├── public/               # Static assets
└── vite.config.ts       # Vite configuration
```

## Development

### Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint

### API Base URL

The API base URL is dynamically constructed based on:
1. Current hostname (for tenant subdomains)
2. Environment variables
3. Fallback to `http://localhost:8083`

## Troubleshooting

### Login Issues

1. **"Invalid credentials"**: 
   - Verify the user exists in the `tenant_users` table
   - Check that the tenant slug matches
   - Ensure password is correct

2. **"Tenant ID is required"**:
   - Make sure you're accessing via tenant subdomain: `{tenant}.localhost:5173`
   - Or set `VITE_TENANT_SLUG` in environment

3. **API Connection Errors**:
   - Verify tenant-service is running on port 8083
   - Check CORS configuration
   - Verify tenant slug in database

### Creating Users

To create a test user, use the PowerShell script:

```powershell
.\create-default-user.ps1 -TenantSlug "yourslug" -Email "admin@test.com"
```

## Security Notes

⚠️ **Default Password**: The default password `adming` should be changed in production!

## License

[Your License Here]

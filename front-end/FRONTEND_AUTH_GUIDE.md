# Frontend Authentication Integration Guide

This guide explains how authentication is integrated in the React frontend with Keycloak and the tenant-service backend.

## Overview

The frontend uses:
- **Keycloak JS** for authentication
- **Single Realm** (`kymatic`) with `tenant_id` claim in JWT tokens
- **React Context** for auth state management
- **API Service** for making authenticated requests to backend services

## Architecture

```
┌─────────────────┐
│   React App     │
│                 │
│  AuthProvider   │───┐
│  (Context)      │   │
└─────────────────┘   │
                       │
                       ▼
┌─────────────────────────────────┐
│      Keycloak (Port 8085)       │
│  Realm: kymatic                  │
│  Client: react-client            │
│  JWT with tenant_id claim        │
└─────────────────────────────────┘
                       │
                       │ JWT Token
                       ▼
┌─────────────────────────────────┐
│   Tenant Service (Port 8083)   │
│   Validates JWT                 │
│   Extracts tenant_id            │
│   Returns tenant data           │
└─────────────────────────────────┘
```

## Key Files

### Authentication
- `src/auth/keycloak.ts` - Keycloak instance creation
- `src/auth/AuthProvider.tsx` - Auth context provider
- `src/auth/useAuth.ts` - Hook to access auth context

### API Integration
- `src/utils/api.ts` - Generic API request utility
- `src/services/tenantService.ts` - Tenant service API client

### Components
- `src/App.tsx` - Main application component
- `src/components/ProtectedRoute.tsx` - Route protection component

## Configuration

### Environment Variables

Create a `.env` file in the `front-end` directory:

```env
VITE_KEYCLOAK_BASE_URL=http://localhost:8085/
VITE_TENANT_SERVICE_URL=http://localhost:8083
```

### Keycloak Client Configuration

The `react-client` in Keycloak must have:
- **Client ID**: `react-client`
- **Public Client**: `true`
- **Standard Flow Enabled**: `true`
- **Valid Redirect URIs**: 
  - `http://localhost:3000/*`
  - `http://localhost:5173/*` (Vite default)
- **Web Origins**: 
  - `http://localhost:3000`
  - `http://localhost:5173`

## How It Works

### 1. Authentication Flow

1. User opens the app
2. `AuthProvider` initializes Keycloak
3. Keycloak checks if user is authenticated
4. If not authenticated, redirects to Keycloak login
5. After login, Keycloak redirects back with authorization code
6. Keycloak exchanges code for JWT token
7. Token is stored and used for API requests

### 2. Token Management

- Token is automatically refreshed before expiration
- Token refresh happens every 15 seconds (checks every 60 seconds before expiry)
- On token expiration, user is redirected to login
- Token is included in all API requests via `Authorization: Bearer <token>` header

### 3. Tenant ID Extraction

The `tenant_id` is extracted from the JWT token claim:
- Set by Keycloak protocol mapper from user attribute
- Stored in `AuthContext` for use throughout the app
- Automatically included in API requests

## Usage Examples

### Using the Auth Hook

```tsx
import { useAuth } from '@auth/useAuth'

function MyComponent() {
  const { isAuthenticated, user, token, tenant, logout } = useAuth()
  
  if (!isAuthenticated) {
    return <div>Not authenticated</div>
  }
  
  return (
    <div>
      <p>Welcome, {user?.preferred_username}</p>
      <p>Tenant: {tenant}</p>
      <button onClick={logout}>Logout</button>
    </div>
  )
}
```

### Making API Requests

```tsx
import { useAuth } from '@auth/useAuth'
import { tenantService } from '@services/tenantService'

function TenantInfo() {
  const { token } = useAuth()
  const [info, setInfo] = useState(null)
  
  useEffect(() => {
    if (token) {
      tenantService.getCurrentUser(token)
        .then(setInfo)
        .catch(console.error)
    }
  }, [token])
  
  return <div>{/* Render info */}</div>
}
```

### Protected Routes

```tsx
import { ProtectedRoute } from '@components/ProtectedRoute'

function App() {
  return (
    <ProtectedRoute requiredRole="admin">
      <AdminPanel />
    </ProtectedRoute>
  )
}
```

## Available API Endpoints

The `tenantService` provides methods for:

- `getCurrentUser(token)` - Get current user info from `/api/me`
- `getCurrentTenant(token)` - Get current tenant ID from `/api/tenant/current`
- `getTenantInfo(token)` - Get tenant information from `/api/tenant/info`
- `listTenants(token, page, size)` - List all tenants (admin only)
- `getTenantById(tenantId, token)` - Get tenant by ID
- `getTenantBySlug(slug, token)` - Get tenant by slug

## Running the Frontend

### Development

```bash
cd front-end
npm install
npm run dev
```

The app will be available at `http://localhost:3000` (or the port configured in `vite.config.ts`).

### Production Build

```bash
cd front-end
npm run build
npm run preview
```

## Testing Authentication

### Test Users

| Username | Password | Tenant ID |
|----------|---------|-----------|
| `user1` | `password` | `tenant1` |
| `user2` | `password` | `tenant2` |
| `admin` | `admin` | `tenant1` |

### Testing Flow

1. Start all services:
   ```bash
   docker-compose up -d
   ```

2. Start frontend:
   ```bash
   cd front-end
   npm run dev
   ```

3. Open browser to `http://localhost:3000`
4. You should be redirected to Keycloak login
5. Login with one of the test users
6. You should be redirected back and see user/tenant information

## Troubleshooting

### CORS Errors

If you see CORS errors:
1. Ensure Keycloak client has correct Web Origins configured
2. Check that tenant-service allows CORS from frontend origin
3. Verify `VITE_KEYCLOAK_BASE_URL` and `VITE_TENANT_SERVICE_URL` are correct

### Token Not Working

1. Check browser console for errors
2. Verify token is being included in requests (check Network tab)
3. Ensure Keycloak is running and accessible
4. Check that user has `tenant_id` attribute set in Keycloak

### Authentication Loop

If stuck in authentication loop:
1. Clear browser cookies/localStorage
2. Check Keycloak redirect URIs match exactly
3. Verify `react-client` is configured correctly
4. Check browser console for Keycloak errors

### Tenant ID Not Showing

1. Verify user has `tenant_id` attribute in Keycloak
2. Check protocol mapper is configured correctly
3. Decode JWT token at [jwt.io](https://jwt.io) to verify `tenant_id` claim
4. Check browser console for errors

## Security Considerations

1. **Never commit tokens** to version control
2. **Use HTTPS in production** - current setup is for development only
3. **Validate tokens on backend** - frontend tokens can be manipulated
4. **Implement token refresh** - already handled automatically
5. **Handle token expiration** - already handled with automatic refresh
6. **Secure storage** - tokens are stored in memory, not localStorage

## Next Steps

- Add role-based UI components
- Implement tenant switching (if needed)
- Add error boundaries for better error handling
- Implement loading states for better UX
- Add unit tests for auth components


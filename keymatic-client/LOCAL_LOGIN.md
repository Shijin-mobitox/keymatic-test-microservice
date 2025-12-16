# Local Login Implementation

The `keymatic-client` now supports local username/password login without showing Keycloak's login page. Users see a clean login form in the application.

## How It Works

1. **Login Form**: Users enter username and password in the application's login page
2. **Password Grant**: The client uses Keycloak's password grant (Resource Owner Password Credentials) to authenticate
3. **JWT Token**: Keycloak returns a JWT token that's stored in localStorage
4. **API Calls**: All API calls include the JWT token in the Authorization header

## Setup

### 1. Enable Password Grant in Keycloak

The `react-client` must have Direct Access Grants enabled. Run the script:

```powershell
.\enable-password-grant.ps1
```

Or manually enable it in Keycloak Admin Console:
- Go to: `http://localhost:8085/admin`
- Login: `admin` / `admin`
- Select Realm: **kymatic**
- Go to: **Clients** → **react-client**
- Under **Settings** tab, enable **Direct Access Grants**
- Click **Save**

### 2. Test Login

1. Start the client: `cd keymatic-client && npm run dev`
2. Navigate to: `http://shijintest123.localhost:5173` (or your tenant subdomain)
3. You'll see the login page (redirects from `/` if not authenticated)
4. Login with:
   - Username: `admin` (or any user in Keycloak)
   - Password: `admin` (or the user's password)

## Features

- ✅ **Clean Login UI**: No Keycloak UI shown to users
- ✅ **Token Management**: Automatic token refresh
- ✅ **Session Persistence**: Tokens stored in localStorage
- ✅ **Auto-redirect**: Redirects to login when not authenticated
- ✅ **Tenant Support**: Extracts tenant ID from JWT token

## Files Changed

- `src/auth/localAuth.ts` - Local authentication service
- `src/auth/AuthProvider.tsx` - Updated to use local auth instead of Keycloak redirect
- `src/components/LoginPage.tsx` - Login form component
- `src/components/LoginPage.css` - Login page styles
- `src/App.tsx` - Added `/login` route
- `src/services/api.ts` - Updated to use local auth tokens

## API Service

The API service automatically:
- Adds the JWT token to all requests
- Refreshes expired tokens
- Redirects to login on authentication failure

## Token Refresh

Tokens are automatically refreshed:
- Before expiration (checks every 5 minutes)
- On 401 responses from API
- Uses refresh token when available

## Logout

Logout clears all stored tokens and redirects to the login page.


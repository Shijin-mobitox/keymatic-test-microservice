# Keycloak Cookie Not Found - 400 Error Fix

## Problem
- Error: `400 Bad Request` when submitting login form
- Keycloak logs show: `error="cookie_not_found"`
- Login form submits from `http://shijintest123.localhost:5173` to `http://localhost:8085`
- Cookies aren't being sent with cross-origin POST requests

## Root Cause
Keycloak's login form submits directly to Keycloak's server (absolute URL), but cookies from the frontend domain aren't being sent because:
1. Cross-origin requests don't send cookies by default
2. Keycloak needs session cookies for CSRF validation
3. The form action URL is server-generated and points directly to Keycloak

## Solution Options

### Option 1: Use Same Origin via Reverse Proxy (RECOMMENDED)

Set up a reverse proxy (nginx/traefik) in front of Keycloak so both frontend and Keycloak appear on the same domain:

```
http://shijintest123.localhost:5173 (Frontend)
http://shijintest123.localhost:5173/auth (Keycloak - proxied)
```

This makes all requests same-origin, so cookies work naturally.

### Option 2: Configure Keycloak to Accept Cross-Origin Cookies

1. **Update docker-compose.yml** (already done):
   ```yaml
   KC_HOSTNAME_STRICT_HTTPS: false
   KC_HOSTNAME_STRICT_BACKCHANNEL: false
   KC_PROXY: edge
   ```

2. **Configure Realm Frontend URL**:
   - Go to Keycloak Admin Console: `http://localhost:8085/admin`
   - Login: `admin` / `admin`
   - Navigate to: **Realm: kymatic** → **Realm Settings** → **General**
   - Set **Frontend URL** to: `http://shijintest123.localhost:5173`
   - Click **Save**

3. **Update Client Configuration**:
   - Navigate to: **Clients** → **react-client**
   - Ensure **Web Origins** includes: `http://shijintest123.localhost:5173` or `+`
   - Ensure **Valid Redirect URIs** includes: `http://shijintest123.localhost:5173/*` or `*`

4. **Clear browser cookies** for both domains and try again

### Option 3: Use Keycloak JS Adapter with Proper Credentials

The Keycloak JS adapter should handle this, but ensure:
- CORS is properly configured (already done)
- The adapter is configured to send credentials

### Option 4: Manual Configuration via Admin Console

Since automated configuration isn't working perfectly, manually configure:

1. **Keycloak Admin Console**: `http://localhost:8085/admin`
2. **Realm Settings** → **General**:
   - **Frontend URL**: `http://shijintest123.localhost:5173`
   - **Backend URL**: (leave blank or set to `http://localhost:8085`)
3. **Save**

## Quick Test

After configuring, test by:
1. Clearing all browser cookies/cache
2. Opening in Incognito mode: `http://shijintest123.localhost:5173`
3. Check browser DevTools → Network tab for cookies in requests

## Current Status

✅ Docker-compose.yml updated with proxy settings
✅ Client CORS configured (webOrigins: +)
✅ Vite proxy configured for /realms, /resources, etc.
⚠️ Need to set Frontend URL in Keycloak Admin Console manually
⚠️ Need to ensure cookies are sent with credentials

## Next Steps

1. **Wait for Keycloak to fully start** (30-60 seconds after restart)
2. **Manually set Frontend URL** in Keycloak Admin Console
3. **Clear browser cache/cookies**
4. **Try accessing the app again**

If still failing, consider Option 1 (reverse proxy) for a more robust solution.


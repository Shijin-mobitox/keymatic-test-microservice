# CORS Error Fix Guide

## Error
```
Access to XMLHttpRequest at 'http://localhost:8085/realms/kymatic/protocol/openid-connect/token' 
from origin 'http://shijintest123.localhost:5173' has been blocked by CORS policy: 
No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

## Root Cause
Keycloak needs to be configured to allow CORS requests from tenant subdomains like `http://shijintest123.localhost:5173`.

## Solution Options

### Option 1: Configure Keycloak via Admin Console (Recommended)

1. **Access Keycloak Admin Console**
   - Go to: `http://localhost:8085`
   - Login with: `admin` / `admin`

2. **Update Client Configuration**
   - Navigate to: **Realm: kymatic** → **Clients** → **react-client**
   - Scroll to **Web Origins** section
   - Set to: `*` or `+` (allows all origins)
   - Scroll to **Valid Redirect URIs**
   - Set to: `*` (allows all redirect URIs)
   - Click **Save**

3. **Clear Browser Cache**
   - Clear browser cache and cookies
   - Or use Incognito/Private mode

4. **Test Again**
   - Access: `http://shijintest123.localhost:5173`

### Option 2: Use Vite Proxy (Alternative Solution)

If CORS issues persist, configure Vite to proxy Keycloak requests:

1. **Update `keymatic-client/vite.config.ts`**:
```typescript
export default defineConfig({
  server: {
    proxy: {
      '/realms': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})
```

2. **Update Keycloak URL in `.env`**:
```env
VITE_KEYCLOAK_BASE_URL=http://localhost:5173
```

This way, Keycloak requests go through the same origin (localhost:5173), avoiding CORS.

### Option 3: Add HTTP Headers Policy (Advanced)

Configure Keycloak HTTP headers policy to allow CORS:

1. Go to Keycloak Admin Console
2. Navigate to: **Realm: kymatic** → **Realm Settings** → **Headers**
3. Add custom headers:
   - `Access-Control-Allow-Origin: *`
   - `Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS`
   - `Access-Control-Allow-Headers: *`

### Option 4: Restart Keycloak

Sometimes configuration changes require a restart:

```bash
docker-compose restart keycloak
```

Wait 30-60 seconds for Keycloak to fully start.

## Verification

After applying the fix, verify:

1. **Check Client Configuration**:
   - Web Origins should show `+` or `*`
   - Redirect URIs should show `*`

2. **Test in Browser Console**:
   ```javascript
   fetch('http://localhost:8085/realms/kymatic/.well-known/openid-configuration', {
     headers: { 'Origin': 'http://shijintest123.localhost:5173' }
   }).then(r => console.log('CORS OK')).catch(e => console.log('CORS ERROR', e))
   ```

3. **Check Network Tab**:
   - Look for `Access-Control-Allow-Origin` header in response
   - Should contain `*` or your origin

## Current Status

The script `fix-keycloak-cors.ps1` has been run and should have:
- ✅ Updated client `webOrigins` to `+` (all origins)
- ✅ Updated client `redirectUris` to `*` (all URIs)

**Next Steps:**
1. Clear browser cache/cookies
2. Try accessing the app again
3. If still failing, use Option 2 (Vite Proxy) as a workaround


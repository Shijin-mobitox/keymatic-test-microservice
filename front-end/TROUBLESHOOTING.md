# Frontend Authentication Troubleshooting Guide

## Issue: Keycloak Redirect Failing

If you're seeing a redirect to Keycloak that fails, here are the steps to fix it:

### 1. Verify Keycloak is Running

```bash
docker-compose ps keycloak
```

Should show status as "Up". If not, start it:
```bash
docker-compose up -d keycloak
```

Wait 30-60 seconds for Keycloak to fully start.

### 2. Check Keycloak Client Configuration

The `react-client` in Keycloak needs proper redirect URIs. You can verify this by:

1. Access Keycloak Admin Console: `http://localhost:8085`
2. Login with: `admin` / `admin`
3. Go to: **Realm: kymatic** → **Clients** → **react-client**
4. Check **Valid Redirect URIs** should include:
   - `http://localhost:3000/*`
   - `http://localhost:5173/*`
   - `http://localhost:3000/`
   - `http://localhost:5173/`
5. Check **Web Origins** should include:
   - `http://localhost:3000`
   - `http://localhost:5173`
   - `+` (for all origins)

### 3. Re-import Realm Configuration

If the client configuration is incorrect, you can re-import the realm:

```bash
# Stop Keycloak
docker-compose stop keycloak

# Remove the realm data (optional, only if you want a fresh start)
docker-compose rm -f keycloak

# Start Keycloak (it will re-import the realm)
docker-compose up -d keycloak
```

### 4. Check Browser Console

Open browser DevTools (F12) and check:
- **Console tab** for JavaScript errors
- **Network tab** for failed requests
- Look for CORS errors or 401/403 responses

### 5. Verify Frontend Configuration

Check that your frontend is using the correct Keycloak URL:

1. Check `.env` file in `front-end/` directory:
   ```env
   VITE_KEYCLOAK_BASE_URL=http://localhost:8085/
   ```

2. Verify the realm name in `src/auth/keycloak.ts`:
   ```typescript
   realm: 'kymatic'
   ```

### 6. Clear Browser Cache

Sometimes cached redirects cause issues:
- Clear browser cache and cookies
- Try incognito/private mode
- Clear localStorage: `localStorage.clear()` in browser console

### 7. Check Keycloak Logs

```bash
docker-compose logs keycloak
```

Look for errors related to:
- Client configuration
- Redirect URI validation
- Authentication failures

### 8. Manual Client Configuration (Alternative)

If the realm import isn't working, configure the client manually:

1. Go to Keycloak Admin Console: `http://localhost:8085`
2. Login: `admin` / `admin`
3. Select realm: **kymatic**
4. Go to: **Clients** → **Create client**
5. Configure:
   - **Client ID**: `react-client`
   - **Client authentication**: OFF (Public client)
   - **Valid redirect URIs**: 
     - `http://localhost:3000/*`
     - `http://localhost:5173/*`
   - **Web origins**: 
     - `http://localhost:3000`
     - `http://localhost:5173`
   - **Standard flow**: ON
   - **Direct access grants**: OFF

### 9. Test Keycloak Directly

Test if Keycloak is accessible:
```bash
curl http://localhost:8085/realms/kymatic/.well-known/openid-configuration
```

Should return JSON with OpenID configuration.

### 10. Common Error Messages

**"Invalid redirect URI"**
- Solution: Add the exact redirect URI to Keycloak client configuration

**"CORS error"**
- Solution: Add the frontend origin to Web Origins in Keycloak client

**"Client not found"**
- Solution: Verify client ID is `react-client` and realm is `kymatic`

**"Realm not found"**
- Solution: Check realm name is `kymatic` and Keycloak has imported it

### 11. Reset Everything

If nothing works, reset Keycloak completely:

```bash
# Stop and remove Keycloak
docker-compose stop keycloak
docker-compose rm -f keycloak

# Remove Keycloak data volume (optional - this deletes all data)
docker volume ls | grep keycloak
# docker volume rm <keycloak-volume-name>

# Start fresh
docker-compose up -d keycloak
```

Wait for Keycloak to start and import the realm (check logs).

## Still Having Issues?

1. Check the exact error message in browser console
2. Check Keycloak logs: `docker-compose logs keycloak`
3. Verify all services are running: `docker-compose ps`
4. Try accessing Keycloak admin console directly
5. Check network connectivity between frontend and Keycloak


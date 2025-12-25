# Keycloak Error Debugging Guide

## Current Error
```
Keycloak: Auth error undefined
Keycloak: Init error undefined
```

## Possible Causes

### 1. Keycloak Server Issues
Keycloak logs show `ClosedChannelException` errors, suggesting:
- Network connectivity issues
- Request timeouts
- Server overload

**Check:**
```powershell
docker-compose logs keycloak --tail 50
docker-compose ps keycloak
```

### 2. CORS Configuration
The frontend might not be able to reach Keycloak due to CORS.

**Check in Keycloak Admin Console:**
1. Go to: `http://localhost:8085/admin`
2. Realm: `kymatic` → Clients → `react-client`
3. Verify **Web Origins** includes: `+` or `http://localhost:3000`

### 3. Realm Not Accessible
The realm might not be properly imported or accessible.

**Test:**
```powershell
Invoke-WebRequest -Uri "http://localhost:8085/realms/kymatic" -UseBasicParsing
```

Should return JSON with realm configuration.

### 4. Client Configuration
The `react-client` might not be properly configured.

**Verify:**
- Client ID: `react-client`
- Public client: `true`
- Standard flow: `enabled`
- Valid redirect URIs: `*` or `http://localhost:3000/*`

## Debugging Steps

### Step 1: Check Keycloak Status
```powershell
docker-compose ps keycloak
docker-compose logs keycloak --tail 50
```

### Step 2: Test Realm Accessibility
```powershell
# Test realm endpoint
Invoke-WebRequest -Uri "http://localhost:8085/realms/kymatic" -UseBasicParsing

# Test token endpoint
Invoke-WebRequest -Uri "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" `
  -Method Post -ContentType "application/x-www-form-urlencoded" `
  -Body @{grant_type="client_credentials";client_id="react-client"}
```

### Step 3: Check Browser Console
Open browser DevTools (F12) → Console tab:
- Look for network errors
- Check for CORS errors
- Look for detailed error messages (now with better logging)

### Step 4: Check Network Tab
1. Open DevTools → Network tab
2. Filter by "keycloak" or "realms"
3. Look for failed requests
4. Check response status codes and error messages

### Step 5: Restart Keycloak
If errors persist:
```powershell
docker-compose restart keycloak
# Wait 30-60 seconds for Keycloak to fully start
```

## Enhanced Error Logging

The code now logs detailed error information:
- Error object details
- Error type and string representation
- Current URL and hash
- Keycloak server URL

Check browser console for these detailed logs to identify the exact issue.

## Common Solutions

### Solution 1: Restart Keycloak
```powershell
docker-compose restart keycloak
```

### Solution 2: Re-import Realm
```powershell
docker-compose down keycloak
docker-compose up -d keycloak
# Wait for realm import
```

### Solution 3: Check CORS
In Keycloak Admin Console:
- Clients → react-client → Settings
- Web Origins: Ensure `+` or `http://localhost:3000` is present

### Solution 4: Clear Browser State
```javascript
// In browser console
localStorage.clear()
sessionStorage.clear()
// Clear cookies for localhost:8085
```

## Next Steps

1. Check the enhanced error logs in browser console
2. Verify Keycloak is accessible
3. Check CORS configuration
4. Restart Keycloak if needed
5. Share the detailed error logs if issue persists






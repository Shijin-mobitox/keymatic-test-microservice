# Keycloak 26.2.0 Upgrade - Nonce Error Fix

## Problem
After upgrading to Keycloak 26.2.0, you may see:
- `[KEYCLOAK] Invalid nonce, clearing token`
- `Keycloak: Init error undefined`
- Authentication loops or failures

## Root Cause
Keycloak 26.x has **stricter nonce validation** and may reject tokens/sessions created with the old version (24.x).

## Solution

### Step 1: Clear Browser Storage (REQUIRED)
**You MUST do this first:**

1. **Open Browser DevTools** (F12)
2. **Application/Storage Tab** → Clear:
   - **Cookies** for `localhost:8085` and `localhost:3000`
   - **Local Storage** - Delete all items
   - **Session Storage** - Delete all items
3. **Or use Incognito/Private mode** for a clean test

### Step 2: Verify Keycloak Client Configuration
1. Go to **Keycloak Admin Console**: `http://localhost:8085/admin`
2. Login: `admin` / `admin`
3. Select realm: **kymatic**
4. Go to: **Clients** → **react-client**
5. Verify these settings:
   - **Access Type**: `public` (not confidential)
   - **Standard Flow Enabled**: `ON`
   - **Direct Access Grants Enabled**: `ON` (if needed)
   - **Valid Redirect URIs**: `*` or `http://localhost:3000/*`
   - **Web Origins**: `+` or `http://localhost:3000`
   - **PKCE Code Challenge Method**: `S256` (should be set automatically)
6. Click **Save**

### Step 3: Code Changes (Already Applied)
The following changes have been made to handle Keycloak 26.x:

**`front-end/src/auth/keycloak.ts`**:
- Added `timeSkew: 0` for stricter time validation
- Added `messageReceiveTimeout: 10000` for better timeout handling

**`front-end/src/auth/AuthProvider.tsx`**:
- Added automatic cleanup of stale Keycloak localStorage items before initialization

### Step 4: Restart Services
```powershell
# Restart Keycloak to ensure clean state
docker-compose restart keycloak

# Wait 30-60 seconds for Keycloak to fully start
# Then refresh your frontend (with cleared storage)
```

### Step 5: Test
1. **Open frontend in Incognito mode**: `http://localhost:3000`
2. You should be redirected to Keycloak login
3. Login with test credentials
4. Should redirect back successfully

## If Still Failing

### Option A: Disable PKCE Temporarily (Testing Only)
In `front-end/src/auth/keycloak.ts`, change:
```typescript
pkceMethod: 'S256',
```
to:
```typescript
// pkceMethod: 'S256', // Temporarily disabled for testing
```

**Note**: This is less secure. Only use for testing, then re-enable.

### Option B: Use Query Mode Instead of Fragment
In `front-end/src/auth/keycloak.ts`, change:
```typescript
responseMode: 'fragment',
```
to:
```typescript
responseMode: 'query',
```

### Option C: Check Keycloak Logs
```powershell
docker-compose logs keycloak | Select-String -Pattern "nonce|error|invalid"
```

## Verification Checklist
- [ ] Browser storage cleared (cookies, localStorage, sessionStorage)
- [ ] Keycloak client `react-client` has PKCE enabled
- [ ] Valid Redirect URIs includes your frontend URL
- [ ] Web Origins includes your frontend origin
- [ ] Keycloak restarted after upgrade
- [ ] Frontend code updated (already done)
- [ ] Tested in Incognito mode

## Expected Behavior After Fix
1. User visits frontend → Redirected to Keycloak login
2. User logs in → Redirected back with token
3. No "Invalid nonce" errors in console
4. Token refresh works correctly

## Additional Notes
- Keycloak 26.x is stricter about:
  - Nonce validation
  - Time synchronization
  - Session state management
- The code changes ensure compatibility with these stricter requirements
- Always clear browser storage when upgrading Keycloak versions


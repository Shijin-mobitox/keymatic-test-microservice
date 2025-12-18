# Redirect Loop Fix - Keycloak 26.2.0

## Problem
After upgrading to Keycloak 26.2.0, the frontend is stuck in a redirect loop between two pages.

## Root Cause
The redirect loop happens because:
1. Keycloak redirects back with a callback in the URL hash (`#state=...&session_state=...`)
2. The callback is detected but not immediately processed
3. The URL cleanup happens too late or triggers another redirect
4. The init logic calls `login()` again even when there's a callback

## Fixes Applied

### 1. Immediate URL Cleanup
- **Before**: URL cleanup happened after 500ms delay
- **Now**: URL cleanup happens **immediately** after token is received
- **Location**: `onAuthSuccess` callback and after `init()` completes

### 2. Better Callback Detection
- **Before**: Only checked for `code=` in URL
- **Now**: Checks for `code=`, `state=`, or `session_state=` in URL hash
- **Location**: `initKeycloak()` function

### 3. Prevent Re-initialization
- **Before**: Could re-initialize even when already authenticated
- **Now**: Checks if already authenticated before initializing
- **Location**: `useEffect` hook

### 4. Removed Explicit redirectUri
- **Before**: Set `redirectUri` explicitly in init options
- **Now**: Let Keycloak use the current URL automatically
- **Location**: `keycloak.ts` - commented out `redirectUri`

### 5. Better Error Handling
- **Before**: Callback errors could cause infinite loops
- **Now**: Timeout check - if callback doesn't process in 2 seconds, show error and clear URL
- **Location**: `initKeycloak()` function

## What Changed

### `front-end/src/auth/AuthProvider.tsx`
1. URL cleanup now happens **immediately** (no delay)
2. Better guards to prevent re-initialization when already authenticated
3. Timeout check for callback processing failures
4. Better error messages

### `front-end/src/auth/keycloak.ts`
1. Removed explicit `redirectUri` setting
2. Let Keycloak automatically use current URL

## Testing the Fix

### Step 1: Clear Browser Storage
```javascript
// In browser console (F12)
localStorage.clear()
sessionStorage.clear()
// Clear cookies for localhost:3000 and localhost:8085
```

### Step 2: Restart Frontend
```powershell
# Stop current server (Ctrl+C)
cd front-end
npm run dev
```

### Step 3: Test in Incognito
1. Open `http://localhost:3000` in **Incognito mode**
2. Should redirect to Keycloak login **once**
3. After login, should redirect back **once**
4. Should **not** redirect again

## Expected Behavior

**✅ Correct Flow:**
1. User opens app → Redirects to Keycloak login (once)
2. User logs in → Redirects back with callback in URL
3. Callback processed → Token received → URL cleaned
4. App loads → **No more redirects**

**❌ Wrong Flow (Redirect Loop):**
1. User opens app → Redirects to Keycloak
2. User logs in → Redirects back
3. Callback detected → But redirects again → Loop continues

## Debugging

If still experiencing redirect loop:

### Check Browser Console
Look for these messages:
- `Keycloak: Initializing...` - Should only appear once
- `Keycloak: Auth success callback` - Should appear after login
- `Keycloak: URL cleaned` - Should appear after callback

### Check Network Tab
1. Open DevTools → Network tab
2. Look for repeated requests to:
   - `/realms/kymatic/protocol/openid-connect/auth` (login)
   - `/realms/kymatic/protocol/openid-connect/token` (token exchange)
3. Should only see these **once** per login

### Check URL
1. After login, URL should be cleaned to: `http://localhost:3000/`
2. Should **not** have hash fragments like `#state=...`
3. If hash persists, the cleanup isn't working

## Additional Troubleshooting

### If redirect loop persists:

1. **Check Keycloak client redirect URI:**
   - Admin Console → Clients → react-client
   - Valid Redirect URIs should include: `*` or `http://localhost:3000/*`

2. **Check for multiple Keycloak instances:**
   ```powershell
   docker-compose ps | findstr keycloak
   ```
   Should only see one Keycloak container

3. **Check Keycloak logs:**
   ```powershell
   docker-compose logs keycloak --tail 50
   ```
   Look for errors or repeated authentication attempts

4. **Try different browser:**
   - Sometimes browser extensions interfere
   - Try Chrome/Firefox/Edge in Incognito

## Summary

The redirect loop was caused by:
- ✅ Delayed URL cleanup allowing re-initialization
- ✅ Explicit redirectUri causing conflicts
- ✅ Missing guards against re-initialization

All of these have been fixed. The app should now:
- ✅ Redirect to login once
- ✅ Process callback once
- ✅ Clean URL immediately
- ✅ Stay on the app page (no more redirects)



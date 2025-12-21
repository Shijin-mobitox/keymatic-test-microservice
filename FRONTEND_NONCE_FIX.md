# Frontend Nonce Error Fix - Complete Solution

## Problem
After upgrading to Keycloak 26.2.0, you're seeing:
- `[KEYCLOAK] Invalid nonce, clearing token`
- `Keycloak: Init error undefined`
- Authentication loops

## Root Cause
Keycloak 26.x has **stricter nonce validation**. Stale browser storage (localStorage, sessionStorage, cookies) from Keycloak 24.x contains invalid nonces that Keycloak 26.x rejects.

## Complete Fix Applied

### 1. Code Changes (Already Done)

**`front-end/src/main.tsx`** - Added cleanup at app startup:
- Clears all Keycloak-related localStorage items (`kc-*`, `*keycloak*`)
- Clears all Keycloak-related sessionStorage items
- Attempts to clear Keycloak cookies
- Runs **before** React renders, ensuring clean state

**`front-end/src/auth/keycloak.ts`** - Updated init options:
- Added `timeSkew: 0` for stricter time validation
- Added `messageReceiveTimeout: 10000` for better timeout handling

**`front-end/src/auth/AuthProvider.tsx`** - Added cleanup before Keycloak init:
- Clears stale localStorage items before creating Keycloak instance
- Prevents nonce conflicts from old sessions

### 2. Manual Steps (YOU MUST DO THIS)

#### Step 1: Clear Browser Storage Completely

**Option A: Use Browser DevTools (Recommended)**
1. Open your frontend: `http://localhost:3000`
2. Press **F12** to open DevTools
3. Go to **Application** tab (Chrome) or **Storage** tab (Firefox)
4. **Clear all**:
   - **Cookies** → Delete all for `localhost:3000` and `localhost:8085`
   - **Local Storage** → Right-click → Clear all
   - **Session Storage** → Right-click → Clear all
5. **Close and reopen** the browser tab/window

**Option B: Use Incognito/Private Mode (Easiest)**
1. Open a new **Incognito/Private** window
2. Navigate to `http://localhost:3000`
3. This gives you a completely clean state

#### Step 2: Restart Frontend Dev Server

```powershell
# Stop the current dev server (Ctrl+C)
# Then restart:
cd front-end
npm run dev
```

#### Step 3: Test in Clean State

1. Open `http://localhost:3000` in **Incognito mode**
2. You should be redirected to Keycloak login
3. Login with test credentials
4. Should redirect back successfully **without nonce errors**

### 3. Verify It's Working

After clearing storage and restarting:

**✅ Success indicators:**
- No "Invalid nonce" errors in console
- Smooth redirect to Keycloak login
- Successful redirect back after login
- Token is stored and used correctly

**❌ If still failing:**
- Check browser console for specific errors
- Verify Keycloak is running: `docker-compose ps keycloak`
- Check Keycloak logs: `docker-compose logs keycloak --tail 50`

## Why This Happens

1. **Keycloak 24.x** stored nonces in browser storage
2. **Keycloak 26.x** validates nonces more strictly
3. Old nonces from 24.x are **invalid** in 26.x
4. The cleanup code removes these stale nonces **before** Keycloak initializes

## Prevention

The cleanup code in `main.tsx` will automatically clear stale state on every app load. However, for the **first time after upgrade**, you must manually clear browser storage once.

## Troubleshooting

### Still seeing "Invalid nonce"?

1. **Double-check browser storage is cleared:**
   - DevTools → Application → Check localStorage (should be empty or no `kc-*` keys)
   - Check sessionStorage (should be empty)
   - Check cookies (no Keycloak cookies)

2. **Verify Keycloak client configuration:**
   - Admin Console → Clients → react-client
   - Ensure "Client authentication" is **OFF** (public client)
   - Ensure PKCE Code Challenge Method is **S256**

3. **Check for multiple Keycloak instances:**
   - Make sure only ONE Keycloak instance is running
   - Check: `docker-compose ps | findstr keycloak`

4. **Try a different browser:**
   - Sometimes browser extensions interfere
   - Try Chrome/Firefox/Edge in Incognito mode

### "Keycloak: Init error undefined"

This usually means:
- Keycloak server is not accessible
- CORS configuration issue
- Network connectivity problem

**Fix:**
- Verify Keycloak is running: `http://localhost:8085/health`
- Check browser console Network tab for failed requests
- Verify CORS settings in Keycloak client configuration

## Summary

✅ **Code changes**: Already applied (cleanup in main.tsx, AuthProvider, keycloak.ts)
✅ **Manual step**: Clear browser storage (one-time, after upgrade)
✅ **Test**: Use Incognito mode for clean test

The nonce error should be **completely resolved** after clearing browser storage!




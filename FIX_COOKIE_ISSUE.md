# Fix: Keycloak Cookie Not Found Error

## Problem
Keycloak login form shows "Cookie not found" error because cookies aren't sent with cross-origin POST requests from `http://shijintest123.localhost:5173` to `http://localhost:8085`.

## Solution: Configure Keycloak Frontend URL Manually

**This MUST be done through the Keycloak Admin Console:**

### Step 1: Access Keycloak Admin Console

1. Open: `http://localhost:8085/admin`
2. Login with: `admin` / `admin`

### Step 2: Configure Realm Frontend URL

1. Select **Realm: kymatic** from the realm dropdown (top-left)
2. Click **Realm Settings** in the left menu
3. Click **General** tab
4. Scroll down to find **Frontend URL** (or look for it in the form)
5. Set it to: `http://shijintest123.localhost:5173`
6. Click **Save** at the bottom

**Note:** If you don't see "Frontend URL" field, try:
- Look in **Realm Settings → General** for any URL fields
- Or check **Realm Settings → Themes** section
- The field might be called "Frontend URL Override" or similar

### Step 3: Verify Client Configuration

1. Go to **Clients** → **react-client**
2. Ensure **Web Origins** includes:
   - `http://shijintest123.localhost:5173`
   - `+` (wildcard for all)
3. Ensure **Valid Redirect URIs** includes:
   - `http://shijintest123.localhost:5173/*`
   - `*` (wildcard)

### Step 4: Clear Browser and Test

1. **Completely clear browser**:
   - Clear all cookies for `localhost:8085` and `shijintest123.localhost:5173`
   - Clear cache
   - Or use **Incognito/Private mode**

2. **Restart Vite dev server** (if running):
   ```bash
   # Stop with Ctrl+C, then:
   cd keymatic-client
   npm run dev
   ```

3. **Wait 30 seconds** for Keycloak to apply changes

4. **Test**: Open `http://shijintest123.localhost:5173` in browser

## Alternative: Use Reverse Proxy (More Robust)

If manual configuration doesn't work, use a reverse proxy to make everything same-origin:

```
Frontend: http://shijintest123.localhost:5173
Keycloak: http://shijintest123.localhost:5173/auth (proxied)
```

This eliminates CORS and cookie issues completely.

## Why This Happens

- Keycloak's login form is server-rendered
- It submits to Keycloak's absolute URL (`http://localhost:8085`)
- Cookies from frontend domain aren't sent cross-origin
- Setting Frontend URL tells Keycloak to generate forms that use the frontend URL

## Current Status

✅ Vite proxy configured for Keycloak requests
✅ Client CORS configured (webOrigins: +)
✅ Docker-compose updated with proxy settings
⚠️ **Need to manually set Frontend URL in Admin Console**

After setting the Frontend URL, cookies should work because Keycloak will generate form actions that use the frontend URL (which goes through the Vite proxy, making it same-origin).


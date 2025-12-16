# FINAL FIX: Keycloak Cookie Error

## The Problem

Keycloak's login form is rendered on Keycloak's domain (`localhost:8085`), but submits from a different origin (`shijintest123.localhost:5173`). Cookies aren't sent cross-origin, causing "Cookie not found" error.

## The Solution: Set Frontend URL in Keycloak

**You MUST do this manually - it's a one-time configuration:**

### Steps:

1. **Open Keycloak Admin Console**:
   ```
   http://localhost:8085/admin
   ```

2. **Login**:
   - Username: `admin`
   - Password: `admin`

3. **Select Realm**:
   - Click the realm dropdown (top-left, currently shows "master" or "kymatic")
   - Select **"kymatic"**

4. **Go to Realm Settings**:
   - Click **"Realm Settings"** in the left sidebar
   - Click **"General"** tab (should be selected by default)

5. **Find Frontend URL Field**:
   - Scroll down through the form
   - Look for **"Frontend URL"** or **"Frontend URL Override"**
   - It might be near the bottom of the form
   - **If you don't see it**: Try looking in **"Attributes"** section or **"Advanced"** section

6. **Set the Value**:
   - Enter: `http://shijintest123.localhost:5173`
   - Or if you want it to work for any tenant subdomain: Leave it blank (Keycloak will use request origin)

7. **Save**:
   - Click the **"Save"** button at the bottom of the page
   - You should see a success message

### After Setting Frontend URL:

1. **Restart Keycloak**:
   ```bash
   docker-compose restart keycloak
   ```

2. **Wait 30-60 seconds** for Keycloak to fully start

3. **Clear browser completely**:
   - Delete all cookies for `localhost:8085` and `shijintest123.localhost:5173`
   - Clear cache
   - Or use **Incognito/Private mode**

4. **Restart Vite dev server**:
   ```bash
   cd keymatic-client
   npm run dev
   ```

5. **Test**: Open `http://shijintest123.localhost:5173`

## Why This Works

When you set the Frontend URL:
- ✅ Keycloak generates login forms that use the frontend URL
- ✅ Form actions point to `http://shijintest123.localhost:5173/realms/...` (goes through Vite proxy)
- ✅ All requests are same-origin (cookies work!)
- ✅ No more CORS or cookie errors

## If Frontend URL Field Doesn't Exist

Some Keycloak versions use attributes instead:

1. In Realm Settings → General
2. Look for **"Attributes"** section
3. Click **"Add"** or edit existing
4. Key: `frontendUrl`
5. Value: `http://shijintest123.localhost:5173`
6. Save

## Verification

After setting it, you can verify by:
1. Going to: `http://localhost:8085/realms/kymatic/account`
2. The URL should redirect or use the frontend URL
3. Login forms should submit to the frontend URL

## Current Status

✅ Vite proxy configured
✅ Client CORS configured  
✅ Form interceptor added
✅ Docker environment variables set
⚠️ **YOU MUST SET FRONTEND URL MANUALLY IN ADMIN CONSOLE**

This is the only way to fix the cookie issue permanently!


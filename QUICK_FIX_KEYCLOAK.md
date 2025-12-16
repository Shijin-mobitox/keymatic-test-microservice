# Quick Fix: Keycloak Cookie Error

## One-Time Manual Configuration (Required)

**This must be done ONCE through the Keycloak Admin Console:**

1. **Open**: `http://localhost:8085/admin`
2. **Login**: `admin` / `admin`
3. **Select Realm**: Choose "kymatic" from dropdown (top-left)
4. **Navigate**: Click "Realm Settings" → "General" tab
5. **Set Frontend URL**: 
   - Look for "Frontend URL" field (may be at the bottom)
   - Set to: `http://shijintest123.localhost:5173`
   - **If field doesn't exist**: Look for "Attributes" section and add `frontendUrl` = `http://shijintest123.localhost:5173`
6. **Save**: Click "Save" button

## After Configuration

1. **Restart Keycloak**:
   ```bash
   docker-compose restart keycloak
   ```

2. **Wait 30 seconds** for Keycloak to fully start

3. **Clear browser completely** (or use Incognito mode)

4. **Restart Vite dev server**:
   ```bash
   cd keymatic-client
   npm run dev
   ```

5. **Test**: Open `http://shijintest123.localhost:5173`

## What This Does

Setting the Frontend URL tells Keycloak to:
- Generate login forms that use the frontend URL
- Accept cookies from the frontend origin
- Generate redirects that go through the Vite proxy (same-origin)

This makes cookies work because all requests appear same-origin through the proxy!


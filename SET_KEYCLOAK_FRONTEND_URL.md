# Set Keycloak Frontend URL - Step by Step

## This is REQUIRED to fix the cookie error

Follow these steps **exactly**:

### Step 1: Open Keycloak Admin Console

1. Open browser: `http://localhost:8085/admin`
2. Click **Administration Console**
3. Login:
   - Username: `admin`
   - Password: `admin`

### Step 2: Select the Realm

1. Look at the **top-left corner** of the page
2. You'll see a dropdown that says "Master" or shows the current realm
3. Click the dropdown and select **"kymatic"**

### Step 3: Navigate to Realm Settings

1. In the **left sidebar menu**, find **"Realm Settings"**
2. Click on it
3. You should see tabs: **General**, **Login**, **Email**, etc.
4. Click the **"General"** tab (should already be selected)

### Step 4: Find and Set Frontend URL

**Option A: Look for "Frontend URL" field**
1. Scroll down in the General tab
2. Look for a field labeled **"Frontend URL"** or **"Frontend URL Override"**
3. Enter: `http://shijintest123.localhost:5173`
4. Click **"Save"** button at the bottom

**Option B: If Frontend URL field doesn't exist**
1. Scroll down to find **"Attributes"** section
2. Click **"Add"** or look for existing attributes
3. Add new attribute:
   - Key: `frontendUrl`
   - Value: `http://shijintest123.localhost:5173`
4. Click **"Save"**

### Step 5: Verify and Restart

1. You should see a success message
2. Restart Keycloak:
   ```bash
   docker-compose restart keycloak
   ```
3. Wait 30-60 seconds for Keycloak to start
4. Clear browser cache/cookies (or use Incognito)
5. Test: `http://shijintest123.localhost:5173`

## Alternative: Set via Environment Variable

If the manual approach doesn't work, we can configure it via Docker environment variable:

```yaml
KC_HOSTNAME_URL: http://shijintest123.localhost:5173
```

But this requires rebuilding the container.

## What This Does

Setting the Frontend URL tells Keycloak:
- ✅ Generate login forms that submit to the frontend URL (through Vite proxy)
- ✅ Accept cookies from the frontend origin
- ✅ Generate redirects using the frontend URL
- ✅ All requests become same-origin (no CORS issues)

After this configuration, cookies will work because all Keycloak requests go through the Vite proxy!


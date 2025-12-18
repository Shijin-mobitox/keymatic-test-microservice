# How to View Keycloak Client Settings

## Step-by-Step Guide

### 1. Access Keycloak Admin Console
- **URL**: `http://localhost:8085/admin`
- **Username**: `admin`
- **Password**: `admin`

### 2. Select the Realm
- Look at the **top-left corner** of the page
- You'll see a dropdown that says **"Master"** or **"kymatic"**
- Click it and select **"kymatic"**

### 3. Navigate to Clients
- In the **left sidebar**, click **"Clients"**
- You'll see a list of clients (gateway-service, react-client, api-service, tenant-service)

### 4. Open react-client
- Click on **"react-client"** in the list
- This opens the client configuration page

### 5. View the Settings Tab
The client page has multiple tabs at the top:
- **Settings** ← **This is where you'll find Access Type**
- **Credentials**
- **Roles**
- **Mappers**
- etc.

### 6. Find Access Type
- Make sure you're on the **"Settings"** tab (should be selected by default)
- Scroll down to find **"Access Type"** field
- It should show: **"public"** (not "confidential" or "bearer-only")

### 7. Find PKCE Code Challenge Method
- Still on the **"Settings"** tab
- Scroll down to find **"Advanced Settings"** section (click to expand if collapsed)
- Look for **"PKCE Code Challenge Method"** field
- It should show: **"S256"** (or "plain" - S256 is recommended)

## Visual Guide

```
Keycloak Admin Console
├── Top-left: Realm dropdown [kymatic ▼]
├── Left Sidebar:
│   ├── Clients ← Click here
│   └── ...
└── Main Content Area:
    └── Clients List
        └── react-client ← Click this
            └── Settings Tab (default)
                ├── Client ID: react-client
                ├── Access Type: [public ▼] ← HERE
                ├── ...
                └── Advanced Settings (expand)
                    └── PKCE Code Challenge Method: [S256 ▼] ← HERE
```

## Quick Check via Browser DevTools

You can also verify the client configuration by checking the realm configuration:

1. Open browser DevTools (F12)
2. Go to **Network** tab
3. In Keycloak Admin Console, go to: **Clients** → **react-client** → **Settings**
4. Look for a network request to `/admin/realms/kymatic/clients/{client-id}`
5. Check the response JSON - you'll see:
   ```json
   {
     "publicClient": true,  // This means Access Type = public
     "attributes": {
       "pkce.code.challenge.method": "S256"  // PKCE method
     }
   }
   ```

## Expected Values

For `react-client`:
- ✅ **Access Type**: `public`
- ✅ **Standard Flow Enabled**: `ON` (checked)
- ✅ **Direct Access Grants Enabled**: `ON` (checked) - optional but useful
- ✅ **Valid Redirect URIs**: `*` or `http://localhost:3000/*`
- ✅ **Web Origins**: `+` or `http://localhost:3000`
- ✅ **PKCE Code Challenge Method**: `S256`

## If Settings Are Different

If you see different values:

1. **Change Access Type to "public"**:
   - Click the **"Access Type"** dropdown
   - Select **"public"**
   - Click **"Save"** at the bottom

2. **Set PKCE Code Challenge Method to "S256"**:
   - Expand **"Advanced Settings"** section
   - Find **"PKCE Code Challenge Method"** dropdown
   - Select **"S256"**
   - Click **"Save"** at the bottom

3. **Verify Redirect URIs**:
   - In **"Valid Redirect URIs"**, ensure you have:
     - `*` (wildcard - allows all)
     - OR `http://localhost:3000/*` (specific)

4. **Verify Web Origins**:
   - In **"Web Origins"**, ensure you have:
     - `+` (allows all)
     - OR `http://localhost:3000` (specific)

## Troubleshooting

**Can't find the Settings tab?**
- Make sure you clicked on the client name (react-client), not just the list
- The Settings tab should be the first/default tab

**Can't find PKCE setting?**
- It's in the **"Advanced Settings"** section
- Scroll down on the Settings tab
- Look for a collapsible section labeled **"Advanced Settings"**
- Click to expand it

**Settings are grayed out/disabled?**
- Make sure you're logged in as an admin user
- Check that you're in the correct realm (kymatic, not master)
- Some settings might be locked if the client is used elsewhere



# How to Find "Access Type" in Keycloak 26.2.0

## The Field Name Changed!

In **Keycloak 26.x**, the "Access Type" field might be labeled differently or in a different location.

## Method 1: Look for "Client authentication" Toggle

In Keycloak 26.2.0, instead of "Access Type", you might see:

1. Go to: **Clients** → **react-client** → **Settings** tab
2. Look for: **"Client authentication"** toggle/switch
   - **OFF** = Public client (what you want)
   - **ON** = Confidential client (not what you want)

**For public client (what you need):**
- ✅ **Client authentication**: **OFF** (disabled)
- This means it's a public client

## Method 2: Check via Browser DevTools (Easiest)

1. Open Keycloak Admin Console: `http://localhost:8085/admin`
2. Go to: **Clients** → **react-client** → **Settings** tab
3. Open **Browser DevTools** (F12)
4. Go to **Network** tab
5. Refresh the page or click somewhere on the Settings tab
6. Look for a request like: `/admin/realms/kymatic/clients/...`
7. Click on it → Go to **Response** tab
8. Look for: `"publicClient": true` ← This confirms it's public

## Method 3: Check the Realm Export (Already Verified)

Your `config/keycloak/realm-export.json` shows:
```json
{
  "clientId": "react-client",
  "publicClient": true,  ← This means Access Type = public ✅
  ...
}
```

**This is already correct!** Your client is configured as public.

## Method 4: Visual Guide for Keycloak 26.x UI

```
Keycloak Admin Console (26.2.0)
│
├─ Clients → react-client
│   │
│   └─ Settings Tab
│       │
│       ├─ Client ID: react-client
│       ├─ Name: (optional)
│       ├─ Description: (optional)
│       │
│       ├─ [Toggle] Client authentication ← THIS IS IT!
│       │   └─ Should be OFF (disabled) for public client
│       │
│       ├─ Standard flow: [✓] Enabled
│       ├─ Direct access grants: [✓] Enabled
│       │
│       └─ Advanced Settings (expand)
│           └─ PKCE Code Challenge Method: S256
```

## What You Should See

**For react-client (public client):**
- ✅ **Client authentication**: **OFF** (toggle disabled/unchecked)
- ✅ **Standard flow**: **ON** (enabled)
- ✅ **Direct access grants**: **ON** (enabled) - optional
- ✅ **Valid redirect URIs**: `*` or `http://localhost:3000/*`
- ✅ **Web origins**: `+` or `http://localhost:3000`
- ✅ **PKCE Code Challenge Method**: `S256` (in Advanced Settings)

## If You See "Client authentication: ON"

This means it's a **confidential** client (wrong for react-client).

**To fix:**
1. Toggle **"Client authentication"** to **OFF**
2. Click **"Save"** at the bottom
3. The "Client secret" section should disappear (confidential clients have secrets, public don't)

## Quick Verification Script

You can also verify via curl:

```powershell
# Get admin token
$adminToken = (Invoke-RestMethod -Uri "http://localhost:8085/realms/master/protocol/openid-connect/token" `
    -Method Post -ContentType "application/x-www-form-urlencoded" `
    -Body @{grant_type="password";client_id="admin-cli";username="admin";password="admin"}).access_token

# Get react-client config
$clientConfig = Invoke-RestMethod -Uri "http://localhost:8085/admin/realms/kymatic/clients?clientId=react-client" `
    -Headers @{Authorization="Bearer $adminToken"}

# Check publicClient
Write-Host "publicClient: $($clientConfig[0].publicClient)" -ForegroundColor $(if($clientConfig[0].publicClient){"Green"}else{"Red"})
```

## Summary

**Your client is already configured correctly** according to the realm export:
- ✅ `publicClient: true` = Public client
- ✅ This is what you need for a frontend SPA

If you can't find the field in the UI, it's likely because:
1. Keycloak 26.x uses different terminology ("Client authentication" toggle instead of "Access Type")
2. The UI layout changed
3. You might be looking at a different tab

**The important thing**: Your configuration is correct! The nonce error should be fixed by clearing browser storage as mentioned in the previous guide.




# 🌐 Frontend Cloud Keycloak Configuration

## ✅ Configuration Complete!

Your React frontend is now configured to use the **cloud Keycloak** instance instead of the local one.

### 🔧 What Was Changed

1. **Updated** `src/auth/keycloak.ts` to use configurable realm and client ID
2. **Created** cloud configuration in `.env` file:
   - **Keycloak URL**: `https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io/`
   - **Client ID**: `kymatic-react-client`
   - **Realm**: `kymatic`

### 🚀 Start Frontend App

```bash
# Make sure you're in the front-end directory
cd front-end

# Install dependencies (if not already done)
npm install

# Start the React development server
npm run dev
# OR
npm start
```

The app will now authenticate against your **cloud Keycloak** instead of `localhost:8085`!

### 🔑 Required: Keycloak Client Configuration

**IMPORTANT**: You need to configure the `kymatic-react-client` in your cloud Keycloak:

1. **Go to**: https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io/
2. **Login**: `admin-gG7X0T1x` / `blEzm8bnafcGnv50`
3. **Select Realm**: `kymatic`
4. **Navigate**: Clients → Create client → `kymatic-react-client`

#### Client Settings:
```
Client ID: kymatic-react-client
Client Type: OpenID Connect
Client authentication: OFF (public client)
Authorization: OFF
Standard flow: ON
Direct access grants: ON
Valid redirect URIs: 
  - http://localhost:3000/*
  - http://localhost:5173/*
  - https://yourdomain.com/*
Valid post logout redirect URIs:
  - http://localhost:3000/
  - http://localhost:5173/
  - https://yourdomain.com/
Web origins:
  - http://localhost:3000
  - http://localhost:5173
  - https://yourdomain.com
```

### 🔄 Switch Between Local and Cloud

To switch back to local Keycloak:
```bash
cp keycloak-config-local.env .env
```

To switch to cloud Keycloak:
```bash
cp keycloak-config-cloud.env .env
```

### 🎯 Expected Results

- ✅ Frontend redirects to **cloud Keycloak** for authentication
- ✅ No more `localhost:8085` redirects
- ✅ Authentication works with cloud-issued JWT tokens
- ✅ tenant-service receives valid tokens from cloud Keycloak

### 🧪 Test Authentication Flow

1. **Start frontend**: `npm run dev`
2. **Open**: http://localhost:3000 or http://localhost:5173
3. **Expect**: Redirect to `https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io/`
4. **Login** with cloud Keycloak credentials
5. **Return** to frontend app authenticated

## 🚨 Troubleshooting

### "Client not found" Error
- Make sure you created `kymatic-react-client` in the cloud Keycloak
- Verify the client ID matches exactly

### CORS Errors
- Check **Web Origins** includes your frontend URL
- Ensure **Valid Redirect URIs** includes your callback URLs

### Still Redirecting to localhost:8085
- Verify `.env` file has the cloud URL
- Restart the frontend development server
- Clear browser cache/localStorage




# 🔧 Keycloak Configuration Guide

Your KyMatic services now support **configurable Keycloak URLs** for easy switching between local and cloud instances.

## 🚀 Quick Setup

### Option 1: Use Cloud Keycloak (Recommended)
```bash
# Copy cloud configuration
cp keycloak-config-cloud.env .env

# Update with your actual admin password
# Edit .env and replace YOUR_CLOUD_ADMIN_PASSWORD with actual password

# Start services
docker-compose up -d
```

### Option 2: Use Local Keycloak
```bash
# Copy local configuration  
cp keycloak-config-local.env .env

# Start services (includes local Keycloak)
docker-compose up -d
```

## 🔄 Environment Variables

All services now use these configurable variables:

| Variable | Local Value | Cloud Value |
|----------|-------------|-------------|
| `KEYCLOAK_BASE_URL` | `http://localhost:8085` | `https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io` |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8085/realms/kymatic` | `https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io/realms/kymatic` |
| `KEYCLOAK_REALM` | `kymatic` | `kymatic` |

## 🏗️ Services Updated

- ✅ **tenant-service** - JWT validation and admin API calls
- ✅ **auth-service** - Authentication flows
- ✅ **gateway-service** - Request routing and auth
- ✅ **api-service** - API authentication

## 🔧 Manual Configuration

You can also set environment variables directly:

```bash
# For cloud Keycloak
export KEYCLOAK_BASE_URL="https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io"
export KEYCLOAK_ISSUER_URI="https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io/realms/kymatic"
export KEYCLOAK_REALM="kymatic"

# Start services
docker-compose up -d
```

## 🎯 Benefits

- **Development**: Use local Keycloak for offline development
- **Testing/Production**: Use cloud Keycloak for better reliability  
- **Organizations API**: Cloud instance likely has newer version without API bugs
- **Zero Code Changes**: Switch between environments with just environment variables

## 🚨 Next Steps

1. **Choose your Keycloak instance** (cloud recommended)
2. **Set up the realm** in your chosen instance
3. **Test the Organizations API** (should work better with cloud)
4. **Configure authentication** in your applications




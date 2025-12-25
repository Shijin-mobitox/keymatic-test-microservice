# 🚀 QA Environment Setup Guide

## Prerequisites
1. ✅ **PostgreSQL running** on host machine (port 5432)
2. ✅ **Cloud Keycloak ready** with `kymatic` realm configured
3. ✅ **Docker and Docker Compose** installed

## Step 1: Configure QA Environment

```bash
# Copy QA configuration
cp keycloak-config-cloud.env .env

# Verify configuration (should show cloud URLs)
cat .env
```

## Step 2: Start Services (excluding local Keycloak)

```bash
# Start only the services we need (not local keycloak)
docker-compose up -d tenant-service auth-service gateway-service api-service workflow-service postgres-db
```

**OR** start tenant-service alone for testing:
```bash
docker-compose up -d postgres-db
docker-compose up tenant-service
```

## Step 3: Verify tenant-service is running

```bash
# Check logs
docker-compose logs -f tenant-service

# Test health endpoint
curl http://localhost:8083/actuator/health
```

## Step 4: Test Keycloak Integration

```bash
# Test tenant creation (should work with cloud Keycloak)
curl -X POST "http://localhost:8083/api/tenants" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "QA Test Tenant",
    "slug": "qatenant",
    "subscriptionTier": "starter",
    "maxUsers": 10,
    "maxStorageGb": 10,
    "adminUser": {
      "email": "admin@qatenant.com",
      "password": "SecurePass123!",
      "firstName": "QA",
      "lastName": "Admin",
      "emailVerified": true
    },
    "metadata": {}
  }'
```

## Expected Results ✅

1. **tenant-service starts successfully** with cloud Keycloak URLs
2. **Organizations API works** (no v26.2.4 bug!)
3. **User-organization assignment succeeds** automatically
4. **JWT validation works** with cloud-issued tokens

## Troubleshooting 🔧

### Issue: "Failed to authenticate with Keycloak"
**Fix**: Verify admin credentials in `.env`:
```bash
KEYCLOAK_ADMIN_USERNAME=admin-gG7X0T1x
KEYCLOAK_ADMIN_PASSWORD=blEzm8bnafcGnv50
```

### Issue: "Connection refused to Keycloak"
**Fix**: Verify Keycloak URLs are external (not Docker internal):
```bash
KEYCLOAK_BASE_URL=https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io
```

### Issue: "Invalid JWT issuer"
**Fix**: Ensure issuer URI matches exactly:
```bash
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io/realms/kymatic
```

## Service URLs 🌐

- **tenant-service**: http://localhost:8083
- **auth-service**: http://localhost:8082  
- **gateway-service**: http://localhost:8081
- **api-service**: http://localhost:8084
- **workflow-service**: http://localhost:8090
- **Cloud Keycloak**: https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io

## Benefits of QA Environment 🎯

- ✅ **No Organizations API bug** (cloud Keycloak is likely newer)
- ✅ **Production-like setup** with external Keycloak
- ✅ **Easy switching** between local/cloud with environment variables
- ✅ **Better reliability** for testing user-organization assignments


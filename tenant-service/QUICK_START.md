# Multi-Tenancy Quick Start Guide

## How Multi-Tenancy Works

The tenant-service automatically extracts the tenant ID from the `X-Tenant-ID` HTTP header and makes it available throughout your code via `TenantContext`.

## Quick Test

### 1. Test Without Tenant ID (should work)
```powershell
Invoke-WebRequest -Uri "http://localhost:8083/"
```

### 2. Test With Tenant ID
```powershell
$headers = @{ "X-Tenant-ID" = "tenant1" }
Invoke-WebRequest -Uri "http://localhost:8083/api/tenant/current" -Headers $headers
```

### 3. Test Tenant-Aware Endpoint
```powershell
$headers = @{ "X-Tenant-ID" = "tenant1" }
Invoke-WebRequest -Uri "http://localhost:8083/api/tenant/info" -Headers $headers
```

## Using TenantContext in Your Code

### In Controllers
```java
@GetMapping("/my-endpoint")
public String myEndpoint() {
    String tenantId = TenantContext.getTenantId();
    // Use tenantId to filter data
    return "Data for tenant: " + tenantId;
}
```

### In Services
```java
@Service
public class MyService {
    public void doSomething() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID required");
        }
        // Your tenant-aware logic here
    }
}
```

## Available Endpoints

| Endpoint | Description | Requires Tenant ID? |
|----------|-------------|---------------------|
| `GET /` | Root endpoint | No (optional) |
| `GET /test` | Test endpoint | No |
| `GET /health` | Health check | No |
| `GET /api/tenant/current` | Get current tenant ID | No (but shows if provided) |
| `GET /api/tenant/info` | Get tenant information | Yes (recommended) |

## Request Examples

### Using cURL (if installed)
```bash
# Without tenant
curl http://localhost:8083/

# With tenant
curl -H "X-Tenant-ID: tenant1" http://localhost:8083/api/tenant/current
```

### Using PowerShell
```powershell
# Without tenant
Invoke-WebRequest -Uri "http://localhost:8083/"

# With tenant
$headers = @{ "X-Tenant-ID" = "tenant1" }
Invoke-WebRequest -Uri "http://localhost:8083/api/tenant/current" -Headers $headers
```

### Using Postman
1. Add header: `X-Tenant-ID` = `tenant1`
2. Send GET request to: `http://localhost:8083/api/tenant/current`

## Key Points

1. **Header Name**: Must be exactly `X-Tenant-ID` (case-sensitive)
2. **Automatic**: Tenant ID is automatically extracted and available via `TenantContext.getTenantId()`
3. **Thread-Safe**: Uses ThreadLocal, so each request has its own tenant context
4. **Optional**: Endpoints can work with or without tenant ID (unless you enforce it)

## Next Steps

- See `MULTI_TENANCY_USAGE.md` for detailed documentation
- Check `TenantAwareController.java` for example implementation
- Review `TenantService.java` for service-layer examples


# Multi-Tenancy Usage Guide

This guide explains how to use the multi-tenancy features in the Tenant Service.

## Overview

The tenant-service implements **header-based multi-tenancy** where the tenant ID is extracted from the `X-Tenant-ID` HTTP header and stored in `TenantContext` (ThreadLocal) for the duration of the request.

## Architecture

1. **TenantFilter**: Automatically extracts tenant ID from `X-Tenant-ID` header
2. **HeaderTenantResolver**: Resolves tenant ID from HTTP request headers
3. **TenantContext**: ThreadLocal storage for current tenant ID
4. **Arconia**: Provides schema-based multi-tenancy support (if configured)

## How It Works

### Request Flow

```
HTTP Request with X-Tenant-ID header
    ↓
TenantFilter intercepts request
    ↓
HeaderTenantResolver extracts tenant ID from header
    ↓
TenantContext.setTenantId(tenantId) - stored in ThreadLocal
    ↓
Request processed by controllers/services
    ↓
TenantContext.getTenantId() - retrieve tenant ID anywhere in code
    ↓
TenantContext.clear() - cleaned up after request
```

## Usage Examples

### 1. Making Requests with Tenant ID

#### Using cURL (Windows PowerShell)

```powershell
# Request with tenant ID
$headers = @{ "X-Tenant-ID" = "tenant1" }
Invoke-WebRequest -Uri "http://localhost:8083/api/tenant/current" -Headers $headers

# Or using curl.exe (if installed)
curl.exe -H "X-Tenant-ID: tenant1" http://localhost:8083/api/tenant/current
```

#### Using Browser (with Extension)

Install a browser extension like "ModHeader" to add custom headers:
- Header Name: `X-Tenant-ID`
- Header Value: `tenant1`

Then visit: `http://localhost:8083/api/tenant/current`

#### Using Postman

1. Create a new request
2. Go to "Headers" tab
3. Add header: `X-Tenant-ID` = `tenant1`
4. Send request to: `http://localhost:8083/api/tenant/current`

### 2. Using TenantContext in Controllers

```java
@RestController
@RequestMapping("/api")
public class MyController {
    
    @GetMapping("/data")
    public String getData() {
        String tenantId = TenantContext.getTenantId();
        
        if (tenantId == null) {
            return "No tenant ID provided";
        }
        
        // Use tenantId to filter data
        return "Data for tenant: " + tenantId;
    }
}
```

### 3. Using TenantContext in Services

```java
@Service
public class MyService {
    
    public void processData() {
        String tenantId = TenantContext.getTenantId();
        
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is required");
        }
        
        // Perform tenant-specific operations
        // With Arconia, database queries are automatically filtered
    }
}
```

### 4. Using TenantContext with JPA/Repositories

When using Arconia with JPA entities:

```java
@Entity
@Table(name = "my_table")
public class MyEntity {
    @Id
    private Long id;
    
    @TenantId  // Arconia annotation - automatically filters by tenant
    private String tenantId;
    
    private String data;
}

@Repository
public interface MyRepository extends JpaRepository<MyEntity, Long> {
    // Queries are automatically filtered by current tenant
    List<MyEntity> findByData(String data);
}
```

## Available Endpoints

### Tenant-Aware Endpoints

- `GET /api/tenant/current` - Get current tenant ID
- `GET /api/tenant/info` - Get tenant information

### Regular Endpoints (No Tenant Required)

- `GET /` - Root endpoint
- `GET /test` - Test endpoint
- `GET /health` - Health check

## Database Schema

The service uses a `tenants` table to store tenant information:

```sql
CREATE TABLE tenants (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    schema_name VARCHAR(255) NOT NULL UNIQUE
);
```

### Setting Up Tenants

Connect to the database and insert tenants:

```sql
INSERT INTO tenants (name, schema_name) VALUES 
    ('Tenant 1', 'tenant1'),
    ('Tenant 2', 'tenant2'),
    ('Acme Corp', 'acme');
```

## Testing Multi-Tenancy

### Test Script (PowerShell)

```powershell
# Test without tenant ID
Write-Host "Testing without tenant ID:" Invoke-WebRequest -Uri "http://localhost:8083/api/tenant/current" | Select-Object -ExpandProperty Content

# Test with tenant ID
Write-Host "`nTesting with tenant ID 'tenant1':" $headers = @{ "X-Tenant-ID" = "tenant1" } Invoke-WebRequest -Uri "http://localhost:8083/api/tenant/current" -Headers $headers | Select-Object -ExpandProperty Content

# Test with different tenant ID
Write-Host "`nTesting with tenant ID 'tenant2':"
$headers = @{ "X-Tenant-ID" = "tenant2" }
Invoke-WebRequest -Uri "http://localhost:8083/api/tenant/current" -Headers $headers | Select-Object -ExpandProperty Content
```

## Best Practices

1. **Always check for null**: Tenant ID might not be provided
   ```java
   String tenantId = TenantContext.getTenantId();
   if (tenantId == null || tenantId.isEmpty()) {
       // Handle missing tenant ID
   }
   ```

2. **Validate tenant exists**: Check if tenant is valid before processing
   ```java
   if (!tenantService.isTenantActive(tenantId)) {
       throw new IllegalArgumentException("Invalid tenant");
   }
   ```

3. **Use Arconia annotations**: For automatic tenant filtering in JPA
   ```java
   @TenantId
   private String tenantId;
   ```

4. **Don't store TenantContext**: It's ThreadLocal and only valid for the current request

5. **Clear context in finally blocks**: If manually setting tenant context
   ```java
   try {
       TenantContext.setTenantId(tenantId);
       // ... operations
   } finally {
       TenantContext.clear();
   }
   ```

## Troubleshooting

### Tenant ID is null

- **Check header name**: Must be exactly `X-Tenant-ID` (case-sensitive)
- **Check filter order**: TenantFilter should run before your controllers
- **Check logs**: Look for "DefaultController initialized" to verify filter is active

### Tenant not found

- **Verify tenant exists**: Check the `tenants` table in database
- **Check schema**: If using schema-based multi-tenancy, verify schema exists

### Requests work without tenant ID

- This is expected for endpoints that don't require tenant context
- Add validation in your service layer if tenant is required

## Configuration

The multi-tenancy is configured in:
- `TenantFilter.java` - Filter that extracts tenant ID
- `HeaderTenantResolver.java` - Resolves tenant from headers
- `FilterConfig.java` - Registers the filter

To change the header name, modify `HeaderTenantResolver.java`:

```java
return req.getHeader("X-Custom-Tenant-Header"); // Change header name here
```


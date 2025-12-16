package com.kymatic.tenantservice.service;

import com.kymatic.shared.multitenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Example service demonstrating how to use TenantContext in service layer.
 * 
 * In a real application, you would use the tenant ID to:
 * - Filter database queries by tenant
 * - Switch database schemas (if using schema-based multi-tenancy with Arconia)
 * - Access tenant-specific configuration
 * - Enforce tenant isolation in business logic
 */
@Service
public class TenantService {
    private static final Logger logger = LoggerFactory.getLogger(TenantService.class);
    
    /**
     * Example method that uses the current tenant ID from TenantContext.
     * The tenant ID is automatically available from the request context
     * (set by TenantFilter from the X-Tenant-ID header).
     */
    public String getTenantSpecificData() {
        String tenantId = TenantContext.getTenantId();
        
        if (tenantId == null || tenantId.isEmpty()) {
            logger.warn("No tenant ID available in context");
            throw new IllegalStateException("Tenant ID is required but not provided");
        }
        
        logger.info("Processing request for tenant: {}", tenantId);
        
        // Example: In a real application, you would:
        // 1. Query database filtered by tenantId
        // 2. Use Arconia to switch to tenant-specific schema
        // 3. Access tenant-specific configuration
        
        return "Data for tenant: " + tenantId;
    }
    
    /**
     * Example method showing how to validate tenant access.
     */
    public boolean isTenantActive(String tenantId) {
        // In a real application, check if tenant exists and is active
        // This would typically query the tenants table
        
        if (tenantId == null || tenantId.isEmpty()) {
            return false;
        }
        
        // Example validation logic
        return tenantId.length() > 0;
    }
    
    /**
     * Example method showing tenant-aware database operations.
     * With Arconia, you can use @TenantId annotation on entities
     * and JPA will automatically filter queries by tenant.
     */
    public void performTenantAwareOperation() {
        String tenantId = TenantContext.getTenantId();
        
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required");
        }
        
        logger.info("Performing operation for tenant: {}", tenantId);
        
        // Example: With Arconia multi-tenancy:
        // - Entities with @TenantId will automatically filter by current tenant
        // - Repository queries will be scoped to the current tenant
        // - Schema switching happens automatically if configured
    }
}


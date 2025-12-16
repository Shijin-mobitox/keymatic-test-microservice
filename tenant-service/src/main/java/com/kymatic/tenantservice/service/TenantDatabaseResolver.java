package com.kymatic.tenantservice.service;

import com.kymatic.shared.multitenancy.TenantContext;
import com.kymatic.tenantservice.persistence.entity.TenantEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service that resolves the current tenant's database name from TenantContext.
 * 
 * This service:
 * - Gets tenant ID from TenantContext (set by JwtTenantResolver filter)
 * - Looks up tenant entity to get database name
 * - Returns database name for use in tenant-specific queries
 */
@Service
public class TenantDatabaseResolver {

    private static final Logger logger = LoggerFactory.getLogger(TenantDatabaseResolver.class);

    private final TenantIdentifierResolver tenantIdentifierResolver;

    public TenantDatabaseResolver(TenantIdentifierResolver tenantIdentifierResolver) {
        this.tenantIdentifierResolver = tenantIdentifierResolver;
    }

    /**
     * Get the current tenant's database name from TenantContext.
     * 
     * @return Database name for the current tenant
     * @throws IllegalStateException if tenant ID is not available in context
     */
    public String getCurrentTenantDatabaseName() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Tenant ID is not available in TenantContext. Ensure X-Tenant-ID header is set.");
        }

        try {
            TenantEntity tenant = tenantIdentifierResolver.resolveTenantEntity(tenantId);
            String databaseName = tenant.getDatabaseName();
            logger.debug("Resolved tenant {} to database {}", tenantId, databaseName);
            return databaseName;
        } catch (Exception e) {
            logger.error("Failed to resolve tenant database for tenant ID: {}", tenantId, e);
            throw new IllegalStateException("Failed to resolve tenant database for tenant: " + tenantId, e);
        }
    }

    /**
     * Get tenant entity for the current tenant from TenantContext.
     */
    public TenantEntity getCurrentTenantEntity() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Tenant ID is not available in TenantContext. Ensure X-Tenant-ID header is set.");
        }
        return tenantIdentifierResolver.resolveTenantEntity(tenantId);
    }
}


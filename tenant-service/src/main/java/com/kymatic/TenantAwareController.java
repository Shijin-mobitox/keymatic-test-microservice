package com.kymatic;

import com.kymatic.shared.multitenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Tenant-aware controller that demonstrates multi-tenant data access.
 * 
 * This controller uses:
 * - JWT tokens from Keycloak for authentication
 * - TenantContext to access the current tenant ID (extracted from JWT)
 * - Arconia for tenant-based data isolation
 */
@RestController
@RequestMapping("/api")
public class TenantAwareController {
    private static final Logger logger = LoggerFactory.getLogger(TenantAwareController.class);
    
    /**
     * Returns tenant-specific information for the authenticated user.
     * 
     * This endpoint:
     * - Requires JWT authentication (validated by Spring Security OAuth2 Resource Server)
     * - Extracts tenant_id from JWT token (via JwtTenantResolver)
     * - Returns user and tenant information
     * 
     * @param jwt The authenticated JWT token
     * @return Map containing user and tenant information
     */
    @GetMapping("/me")
    public Map<String, Object> getMe(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = TenantContext.getTenantId();
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        String subject = jwt.getSubject();
        
        logger.info("User '{}' (subject: {}) accessing tenant: {}", username, subject, tenantId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("subject", subject);
        response.put("username", username);
        response.put("email", email);
        response.put("tenantId", tenantId);
        response.put("tenantIdFromJwt", jwt.getClaimAsString("tenant_id"));
        response.put("message", "This is tenant-specific data for tenant: " + tenantId);
        
        // In a real application, you would:
        // 1. Query tenant-specific data from the database
        // 2. Use Arconia's tenant-aware repositories
        // 3. Return tenant-scoped resources
        
        return response;
    }
    
    /**
     * Get the current tenant ID from the request context.
     * The tenant ID is automatically extracted from the JWT token
     * by the JwtTenantResolver filter and stored in TenantContext.
     * 
     * @return Current tenant ID or error message if not provided
     */
    @GetMapping("/tenant/current")
    public String getCurrentTenant() {
        String tenantId = TenantContext.getTenantId();
        logger.info("Current tenant ID: {}", tenantId);
        
        if (tenantId == null || tenantId.isEmpty()) {
            return "No tenant ID provided. The JWT token must include a 'tenant_id' claim.";
        }
        
        return "Current tenant ID: " + tenantId;
    }
    
    /**
     * Example endpoint that demonstrates tenant-aware data access.
     * In a real scenario, you would use the tenantId to:
     * - Filter database queries by tenant
     * - Switch database schemas (if using schema-based multi-tenancy)
     * - Access tenant-specific resources
     */
    @GetMapping("/tenant/info")
    public TenantInfo getTenantInfo() {
        String tenantId = TenantContext.getTenantId();
        logger.info("Getting info for tenant: {}", tenantId);
        
        if (tenantId == null || tenantId.isEmpty()) {
            return new TenantInfo(null, "No tenant ID provided", false);
        }
        
        // In a real application, you would:
        // 1. Look up tenant details from the database
        // 2. Validate tenant exists and is active
        // 3. Return tenant-specific information
        
        return new TenantInfo(tenantId, "Tenant is active", true);
    }
    
    /**
     * Simple DTO for tenant information
     */
    public static class TenantInfo {
        private String tenantId;
        private String status;
        private boolean active;
        
        public TenantInfo(String tenantId, String status, boolean active) {
            this.tenantId = tenantId;
            this.status = status;
            this.active = active;
        }
        
        public String getTenantId() {
            return tenantId;
        }
        
        public String getStatus() {
            return status;
        }
        
        public boolean isActive() {
            return active;
        }
    }
}


package com.kymatic.authservice.controller;

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
        response.put("service", "auth-service");
        
        // In a real application, you would:
        // 1. Query database filtered by tenantId
        // 2. Use Arconia to switch to tenant-specific schema
        // 3. Access tenant-specific configuration
        
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
}


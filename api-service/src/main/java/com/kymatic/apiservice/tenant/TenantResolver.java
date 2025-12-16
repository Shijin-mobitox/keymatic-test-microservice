package com.kymatic.apiservice.tenant;

import com.kymatic.shared.multitenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TenantResolver filter that extracts tenant_id from JWT tokens.
 * 
 * This filter:
 * - Runs after JWT authentication
 * - Extracts the 'tenant_id' claim from the validated JWT token
 * - Stores the tenant_id in TenantContext (ThreadLocal) for use throughout the request
 * - Cleans up TenantContext after request processing
 * 
 * The tenant_id claim is added to JWT tokens by Keycloak via a protocol mapper.
 * This enables multi-tenancy where each user's token includes their tenant identifier.
 */
@Component
public class TenantResolver extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantResolver.class);
    private static final String TENANT_ID_CLAIM = "tenant_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Get the authentication object from Spring Security context
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // Check if authentication is a JWT token
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                
                // Extract tenant_id claim from JWT
                String tenantId = jwt.getClaimAsString(TENANT_ID_CLAIM);
                
                if (tenantId != null && !tenantId.isEmpty()) {
                    // Store tenant_id in TenantContext for use throughout the request
                    TenantContext.setTenantId(tenantId);
                    logger.debug("Tenant ID extracted from JWT: {}", tenantId);
                } else {
                    logger.warn("JWT token does not contain 'tenant_id' claim. Request: {}", request.getRequestURI());
                }
            } else {
                // Not a JWT authentication - might be a public endpoint
                logger.debug("No JWT authentication found for request: {}", request.getRequestURI());
            }
            
            // Continue with the filter chain
            filterChain.doFilter(request, response);
            
        } finally {
            // Always clear TenantContext after request processing
            // This ensures no tenant context leaks between requests
            TenantContext.clear();
        }
    }
}

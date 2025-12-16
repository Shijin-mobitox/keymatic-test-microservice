package com.kymatic.tenantservice.multitenancy;

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
public class JwtTenantResolver extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTenantResolver.class);
    
    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String TENANT_HEADER = "X-Tenant-ID";
    
    // Admin/management endpoints that should always use master database (bypass tenant routing)
    // These endpoints are used by the admin panel and should never route to tenant databases
    private static final String[] MASTER_DB_ONLY_PATTERNS = {
        "/api/tenants",           // Tenant management (create, list, get tenant)
        "/api/tenant-users",      // Tenant user management in master DB
        "/api/subscriptions",     // Subscription management
        "/api/audit-logs",        // Audit log viewing (admin querying tenant logs)
        "/api/auth",              // Authentication endpoints (login, refresh) - use master DB
        "/api/me",                // User info endpoint (reads from JWT, no DB query)
        "/api/tenant/current",    // Current tenant endpoint (reads from context)
        "/api/tenant/info",       // Tenant info endpoint (reads from context)
        "/actuator"               // Spring Boot actuator endpoints
    };

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Skip tenant routing for admin/management endpoints - they always use master DB
            String requestPath = request.getRequestURI();
            if (isMasterDbOnlyEndpoint(requestPath)) {
                logger.debug("JwtTenantResolver: Admin/management endpoint detected: {}. Skipping tenant routing (will use master DB)", requestPath);
                // Ensure tenant ID is not set (clear in case it was set by a previous filter)
                TenantContext.clear();
                filterChain.doFilter(request, response);
                return;
            }
            
            // Get the authentication object from Spring Security context
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            
            String tenantId = null;

            // Prefer tenant_id claim from JWT when available
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                tenantId = jwt.getClaimAsString(TENANT_ID_CLAIM);

                if (tenantId != null && !tenantId.isEmpty()) {
                    logger.debug("Tenant ID extracted from JWT: {}", tenantId);
                } else {
                    logger.warn("JWT token does not contain '{}' claim. Request: {}", TENANT_ID_CLAIM, request.getRequestURI());
                }
            } else {
                // Not a JWT authentication - might be a public endpoint
                logger.debug("No JWT authentication found for request: {}", request.getRequestURI());
            }

            // Fallback: allow tenant to be provided via header or query parameter
            if (tenantId == null || tenantId.isEmpty()) {
                String headerTenant = request.getHeader(TENANT_HEADER);
                if (headerTenant != null && !headerTenant.isEmpty()) {
                    tenantId = headerTenant;
                    logger.debug("Tenant ID extracted from header {}: {}", TENANT_HEADER, tenantId);
                } else {
                    String paramTenant = request.getParameter("tenant");
                    if (paramTenant == null || paramTenant.isEmpty()) {
                        paramTenant = request.getParameter("tenantId");
                    }
                    if (paramTenant != null && !paramTenant.isEmpty()) {
                        tenantId = paramTenant;
                        logger.debug("Tenant ID extracted from request parameter: {}", tenantId);
                    }
                }
            }

            if (tenantId != null && !tenantId.isEmpty()) {
                // Store tenant_id in TenantContext for use throughout the request
                TenantContext.setTenantId(tenantId);
            }
            
            // Continue with the filter chain
            filterChain.doFilter(request, response);
            
        } finally {
            // Always clear TenantContext after request processing
            // This ensures no tenant context leaks between requests
            TenantContext.clear();
        }
    }
    
    /**
     * Check if the request path matches an admin/management endpoint that should use master DB only.
     */
    private boolean isMasterDbOnlyEndpoint(String requestPath) {
        if (requestPath == null || requestPath.isEmpty()) {
            return false;
        }
        for (String pattern : MASTER_DB_ONLY_PATTERNS) {
            if (requestPath.startsWith(pattern)) {
                return true;
            }
        }
        return false;
    }
}


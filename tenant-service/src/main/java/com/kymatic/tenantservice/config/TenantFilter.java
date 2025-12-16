package com.kymatic.tenantservice.config;

import com.kymatic.shared.multitenancy.TenantContext;
import com.kymatic.shared.multitenancy.TenantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that extracts tenant ID from headers (X-Tenant-ID).
 * 
 * NOTE: This filter works in conjunction with JwtTenantResolver.
 * Both filters skip tenant routing for admin/management endpoints.
 */
@Component
public class TenantFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantFilter.class);
    
    // Admin/management endpoints that should always use master database (bypass tenant routing)
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
    
    @Autowired
    private TenantResolver tenantResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // Skip tenant routing for admin/management endpoints - they always use master DB
            String requestPath = request.getRequestURI();
            if (isMasterDbOnlyEndpoint(requestPath)) {
                logger.debug("TenantFilter: Admin/management endpoint detected: {}. Skipping tenant routing (will use master DB)", requestPath);
                // Ensure tenant ID is not set (clear in case it was set by a previous filter)
                TenantContext.clear();
                filterChain.doFilter(request, response);
                return;
            }
            
            // For non-admin endpoints, extract tenant ID from headers
            String tenantId = tenantResolver.resolveTenantId(request);
            if (tenantId != null && !tenantId.isEmpty()) {
                TenantContext.setTenantId(tenantId);
                logger.debug("TenantFilter: Tenant ID {} set from header for request: {}", tenantId, requestPath);
            }
            
            filterChain.doFilter(request, response);
        } finally {
            // Always clear TenantContext after request processing
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

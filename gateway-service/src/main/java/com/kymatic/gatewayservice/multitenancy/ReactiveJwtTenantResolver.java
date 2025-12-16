package com.kymatic.gatewayservice.multitenancy;

import com.kymatic.shared.multitenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive TenantResolver filter that extracts tenant_id from JWT tokens.
 * 
 * This filter:
 * - Runs after JWT authentication in the reactive chain
 * - Extracts the 'tenant_id' claim from the validated JWT token
 * - Stores the tenant_id in TenantContext (ThreadLocal) for use throughout the request
 * - Cleans up TenantContext after request processing
 * 
 * The tenant_id claim is added to JWT tokens by Keycloak via a protocol mapper.
 * This enables multi-tenancy where each user's token includes their tenant identifier.
 * 
 * Note: In reactive applications, we use Reactor's context to propagate tenant information,
 * but we still use ThreadLocal for compatibility with Arconia and other blocking libraries.
 */
@Component
public class ReactiveJwtTenantResolver implements WebFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(ReactiveJwtTenantResolver.class);
    private static final String TENANT_ID_CLAIM = "tenant_id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .cast(Authentication.class)
            .filter(auth -> auth instanceof JwtAuthenticationToken)
            .cast(JwtAuthenticationToken.class)
            .map(JwtAuthenticationToken::getToken)
            .doOnNext(jwt -> {
                // Extract tenant_id claim from JWT
                String tenantId = jwt.getClaimAsString(TENANT_ID_CLAIM);
                
                if (tenantId != null && !tenantId.isEmpty()) {
                    // Store tenant_id in TenantContext for use throughout the request
                    TenantContext.setTenantId(tenantId);
                    logger.debug("Tenant ID extracted from JWT: {}", tenantId);
                } else {
                    logger.warn("JWT token does not contain 'tenant_id' claim. Request: {}", 
                            exchange.getRequest().getURI());
                }
            })
            .then(chain.filter(exchange))
            .doFinally(signalType -> {
                // Always clear TenantContext after request processing
                // This ensures no tenant context leaks between requests
                TenantContext.clear();
            });
    }
}


package com.kymatic.gatewayservice.config;

import com.kymatic.gatewayservice.multitenancy.ReactiveJwtTenantResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Reactive Security configuration for OAuth2 Resource Server.
 * 
 * This configuration:
 * - Enables JWT validation from Keycloak (reactive)
 * - Configures OAuth2 Resource Server for WebFlux
 * - Integrates ReactiveJwtTenantResolver filter to extract tenant_id from JWT
 * - Sets up endpoint authorization rules
 */
@Configuration
@EnableWebFluxSecurity
public class ReactiveSecurityConfig {

    private final ReactiveJwtTenantResolver reactiveJwtTenantResolver;

    public ReactiveSecurityConfig(ReactiveJwtTenantResolver reactiveJwtTenantResolver) {
        this.reactiveJwtTenantResolver = reactiveJwtTenantResolver;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            // Disable CSRF for stateless JWT-based authentication
            .csrf(csrf -> csrf.disable())
            
            // Configure OAuth2 Resource Server for JWT validation (reactive)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                    // JWT decoder is auto-configured from application.yml
                    // issuer-uri and jwk-set-uri are used to validate tokens
                })
            )
            
            // Configure authorization rules
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints (no authentication required)
                .pathMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/hello"
                ).permitAll()
                
                // All other endpoints require authentication
                .anyExchange().authenticated()
            )
            
            // Add ReactiveJwtTenantResolver filter after JWT authentication
            // This extracts tenant_id from the JWT and stores it in TenantContext
            .addFilterAfter(reactiveJwtTenantResolver, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }
}


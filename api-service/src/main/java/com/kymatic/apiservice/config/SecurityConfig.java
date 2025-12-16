package com.kymatic.apiservice.config;

import com.kymatic.apiservice.tenant.TenantResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for OAuth2 Resource Server.
 * 
 * This configuration:
 * - Enables JWT validation from Keycloak
 * - Configures OAuth2 Resource Server
 * - Integrates TenantResolver filter to extract tenant_id from JWT
 * - Sets up endpoint authorization rules
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TenantResolver tenantResolver;

    public SecurityConfig(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless JWT-based authentication
            .csrf(csrf -> csrf.disable())
            
            // Configure session management to be stateless (JWT-based)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure OAuth2 Resource Server for JWT validation
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                    // JWT decoder is auto-configured from application.yml
                    // issuer-uri and jwk-set-uri are used to validate tokens
                })
            )
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints (no authentication required)
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/hello"
                ).permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Add TenantResolver filter after JWT authentication
            // This extracts tenant_id from the JWT and stores it in TenantContext
            .addFilterAfter(tenantResolver, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

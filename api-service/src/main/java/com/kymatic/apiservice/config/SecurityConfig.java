package com.kymatic.apiservice.config;

import com.kymatic.apiservice.tenant.TenantResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

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
                .jwt(jwt -> jwt.decoder(keycloakJwtDecoder()))
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

    /**
     * Creates Keycloak JWT decoder that accepts tokens from both internal and external Keycloak URLs.
     * 
     * The frontend uses http://localhost:8085 to authenticate, so tokens have issuer:
     *   http://localhost:8085/realms/kymatic
     * 
     * But the service running in Docker can only reach Keycloak via:
     *   http://keycloak:8080/realms/kymatic
     * 
     * This decoder:
     * - Uses the internal Keycloak URL to fetch JWK keys (for validation)
     * - Accepts tokens with issuer from either URL
     */
    @Bean
    public JwtDecoder keycloakJwtDecoder() {
        // Use internal Keycloak URL to fetch JWK keys (service can reach this)
        // NimbusJwtDecoder automatically caches JWK keys to improve performance
        String jwkSetUri = "http://keycloak:8080/realms/kymatic/protocol/openid-connect/certs";
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // Create a custom validator that accepts tokens from either issuer URI
        OAuth2TokenValidator<Jwt> issuerValidator = new OAuth2TokenValidator<Jwt>() {
            private static final String INTERNAL_ISSUER = "http://keycloak:8080/realms/kymatic";
            private static final String EXTERNAL_ISSUER = "http://localhost:8085/realms/kymatic";

            @Override
            public OAuth2TokenValidatorResult validate(Jwt token) {
                String issuer = token.getIssuer().toString();
                
                // Accept tokens from either issuer
                if (INTERNAL_ISSUER.equals(issuer) || EXTERNAL_ISSUER.equals(issuer)) {
                    return OAuth2TokenValidatorResult.success();
                }
                
                // If issuer doesn't match either, return error
                OAuth2Error error = new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_TOKEN,
                    "The token's issuer is not valid. Expected: " + INTERNAL_ISSUER + " or " + EXTERNAL_ISSUER + ", but was: " + issuer,
                    null
                );
                return OAuth2TokenValidatorResult.failure(error);
            }
        };

        // Combine custom issuer validator with timestamp validator (exp, nbf)
        // We don't use JwtValidators.createDefault() because it includes issuer validation
        OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(
            List.of(issuerValidator, timestampValidator)
        );

        jwtDecoder.setJwtValidator(combinedValidator);
        return jwtDecoder;
    }
}

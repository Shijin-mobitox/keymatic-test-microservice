package com.kymatic.tenantservice.config;

import com.kymatic.tenantservice.multitenancy.JwtTenantResolver;
import com.kymatic.tenantservice.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
@Profile("!dev") // Only active when NOT in dev profile
public class SecurityConfig {

    private final JwtTenantResolver jwtTenantResolver;
    private final JwtTokenUtil jwtTokenUtil;

    @Value("${app.jwt.issuer:http://localhost:8083}")
    private String localJwtIssuer;
    
    @Value("${keycloak.admin.server-url:http://localhost:8085}")
    private String keycloakServerUrl;
    
    @Value("${keycloak.admin.realm:kymatic}")
    private String keycloakRealm;

    public SecurityConfig(JwtTenantResolver jwtTenantResolver, JwtTokenUtil jwtTokenUtil) {
        this.jwtTenantResolver = jwtTenantResolver;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS for SPA/local dev
            .cors(cors -> {})
            // Disable CSRF for stateless JWT-based authentication
            .csrf(csrf -> csrf.disable())
            
            // Configure session management to be stateless (JWT-based)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure OAuth2 Resource Server for JWT validation
            // Uses composite decoder that supports both Keycloak and local tokens
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(compositeJwtDecoder()))
            )
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints (no authentication required)
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/swagger-ui/index.html",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/api/auth/login",
                    "/api/auth/refresh"
                ).permitAll()
                // Allow tenant creation without authentication (signup/onboarding)
                .requestMatchers(HttpMethod.POST, "/api/tenants").permitAll()
                // Allow listing tenants without authentication (for tenant selection)
                .requestMatchers(HttpMethod.GET, "/api/tenants").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Add TenantResolver filter after JWT authentication
            // This extracts tenant_id from the JWT and stores it in TenantContext
            .addFilterAfter(jwtTenantResolver, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Composite JWT decoder that supports both Keycloak tokens and local authentication tokens.
     * 
     * Supports:
     * - Keycloak tokens (RS256 signed with JWKS from Keycloak)
     * - Local tokens (HS256 signed with shared secret from JwtTokenUtil)
     * 
     * This allows the system to support both authentication methods simultaneously.
     */
    @Bean
    public JwtDecoder compositeJwtDecoder() {
        // Create Keycloak decoder (for RS256 tokens from Keycloak)
        JwtDecoder keycloakDecoder = createKeycloakDecoder();
        
        // Create composite decoder that tries Keycloak first, then local
        return new CompositeJwtDecoder(keycloakDecoder, jwtTokenUtil, localJwtIssuer);
    }

    /**
     * Creates Keycloak JWT decoder that accepts tokens from configurable Keycloak URLs.
     * 
     * This decoder:
     * - Uses configurable Keycloak URL to fetch JWK keys (for validation)
     * - Accepts tokens from the configured issuer URL
     * - Supports both local and cloud Keycloak instances
     */
    private JwtDecoder createKeycloakDecoder() {
        // Use configured Keycloak URL to fetch JWK keys
        String jwkSetUri = String.format("%s/realms/%s/protocol/openid-connect/certs", keycloakServerUrl, keycloakRealm);
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // Create a custom validator that accepts tokens from the configured issuer
        OAuth2TokenValidator<Jwt> issuerValidator = new OAuth2TokenValidator<Jwt>() {
            private final String expectedIssuer = String.format("%s/realms/%s", keycloakServerUrl, keycloakRealm);

            @Override
            public OAuth2TokenValidatorResult validate(Jwt token) {
                String issuer = token.getIssuer().toString();
                
                // Accept tokens from the configured issuer
                if (expectedIssuer.equals(issuer)) {
                    return OAuth2TokenValidatorResult.success();
                }
                
                // If issuer doesn't match, return error
                OAuth2Error error = new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_TOKEN,
                    "The token's issuer is not valid. Expected: " + expectedIssuer + ", but was: " + issuer,
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

    /**
     * Password encoder for hashing passwords
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Permissive CORS for local development:
     * - Allows all origins and headers
     * - Allows Authorization header so SPA can send Bearer tokens
     * Lock this down in production.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Location"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}


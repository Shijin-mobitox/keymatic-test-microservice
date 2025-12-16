package com.kymatic.tenantservice.config;

import org.springframework.context.annotation.Configuration;

/**
 * Filter configuration for tenant resolution.
 * 
 * Note: The JwtTenantResolver is now configured in SecurityConfig
 * and runs as part of the Spring Security filter chain after JWT authentication.
 * 
 * The old header-based TenantFilter is no longer needed since we're using
 * JWT-based tenant resolution from Keycloak tokens.
 */
@Configuration
public class FilterConfig {
    // JwtTenantResolver is configured in SecurityConfig
    // No additional filter registration needed
}

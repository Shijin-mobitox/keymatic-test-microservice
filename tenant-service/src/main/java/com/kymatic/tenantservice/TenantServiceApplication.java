package com.kymatic.tenantservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application class for the multi-tenant authentication microservice.
 * 
 * This service integrates:
 * - Spring Boot 3.x for the application framework
 * - Arconia framework for multi-tenant data isolation
 * - Keycloak for authentication and authorization (OAuth2/OIDC)
 * - Spring Security OAuth2 Resource Server for JWT validation
 * - PostgreSQL for data persistence
 * 
 * Multi-tenancy is achieved by:
 * 1. Keycloak issues JWT tokens with a 'tenant_id' claim (via protocol mapper)
 * 2. JwtTenantResolver filter extracts tenant_id from JWT and stores it in TenantContext
 * 3. Arconia uses TenantContext to provide tenant-aware data access
 * 4. All database operations are automatically scoped to the current tenant
 * 
 * @see com.kymatic.tenantservice.multitenancy.JwtTenantResolver
 * @see com.kymatic.tenantservice.config.SecurityConfig
 * @see com.kymatic.shared.multitenancy.TenantContext
 */
@SpringBootApplication(scanBasePackages = "com.kymatic")
@EnableFeignClients(basePackages = "com.kymatic")
public class TenantServiceApplication {
    private static final Logger logger = LoggerFactory.getLogger(TenantServiceApplication.class);
    
    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(TenantServiceApplication.class, args);
        
        // Log all registered controllers for debugging
        logger.info("=== Registered REST Controllers ===");
        ctx.getBeansWithAnnotation(org.springframework.web.bind.annotation.RestController.class)
            .forEach((name, bean) -> logger.info("RestController: {} -> {}", name, bean.getClass().getName()));
        
        logger.info("=== Multi-Tenant Authentication Service started successfully ===");
        logger.info("Keycloak integration: Enabled");
        logger.info("Arconia multi-tenancy: Enabled");
    }
}

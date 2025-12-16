package com.kymatic.tenantservice.integration;

import com.kymatic.shared.multitenancy.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for tenant-service using Testcontainers.
 * 
 * This test suite:
 * - Starts Keycloak container with test realm
 * - Starts PostgreSQL container for database
 * - Tests JWT authentication and tenant resolution
 * - Validates tenant-aware endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
class TenantServiceIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    // Keycloak container for authentication testing
    @Container
    static GenericContainer<?> keycloak = new GenericContainer<>(
            DockerImageName.parse("quay.io/keycloak/keycloak:24.0.2"))
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .withCommand("start-dev")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/health").forPort(8080)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    // PostgreSQL container for database
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16"))
            .withDatabaseName("tenant_test")
            .withUsername("tenant")
            .withPassword("tenant");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure Keycloak issuer URI
        String keycloakUrl = "http://localhost:" + keycloak.getMappedPort(8080);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloakUrl + "/realms/kymatic");
        
        // Configure database
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        // Clear TenantContext before each test
        TenantContext.clear();
        
        // Setup MockMvc with Spring Security
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testGetMeEndpointWithTenantId() throws Exception {
        // Create a JWT token with tenant_id claim
        mockMvc.perform(get("/api/me")
                .with(jwt().jwt(jwt -> {
                    jwt.claim("sub", "user-123");
                    jwt.claim("preferred_username", "testuser");
                    jwt.claim("email", "testuser@example.com");
                    jwt.claim("tenant_id", "tenant1");
                }))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("user-123"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("testuser@example.com"))
                .andExpect(jsonPath("$.tenantId").value("tenant1"))
                .andExpect(jsonPath("$.tenantIdFromJwt").value("tenant1"));
    }

    @Test
    void testGetMeEndpointWithoutTenantId() throws Exception {
        // JWT token without tenant_id claim
        mockMvc.perform(get("/api/me")
                .with(jwt().jwt(jwt -> {
                    jwt.claim("sub", "user-123");
                    jwt.claim("preferred_username", "testuser");
                    jwt.claim("email", "testuser@example.com");
                    // No tenant_id claim
                }))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("user-123"))
                .andExpect(jsonPath("$.tenantId").isEmpty());
    }

    @Test
    void testGetMeEndpointUnauthenticated() throws Exception {
        // Request without JWT token should be rejected
        mockMvc.perform(get("/api/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetCurrentTenantWithTenantId() throws Exception {
        mockMvc.perform(get("/api/tenant/current")
                .with(jwt().jwt(jwt -> {
                    jwt.claim("tenant_id", "tenant1");
                })))
                .andExpect(status().isOk())
                .andExpect(content().string("Current tenant ID: tenant1"));
    }

    @Test
    void testGetCurrentTenantWithoutTenantId() throws Exception {
        mockMvc.perform(get("/api/tenant/current")
                .with(jwt().jwt(jwt -> {
                    // No tenant_id claim
                })))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "No tenant ID provided")));
    }

    @Test
    void testGetTenantInfoWithTenantId() throws Exception {
        mockMvc.perform(get("/api/tenant/info")
                .with(jwt().jwt(jwt -> {
                    jwt.claim("tenant_id", "tenant1");
                }))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant1"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void testPublicEndpointsWithoutAuthentication() throws Exception {
        // Health endpoint should be accessible without authentication
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testDifferentTenants() throws Exception {
        // Test tenant1
        mockMvc.perform(get("/api/me")
                .with(jwt().jwt(jwt -> {
                    jwt.claim("sub", "user-1");
                    jwt.claim("tenant_id", "tenant1");
                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant1"));

        // Test tenant2
        mockMvc.perform(get("/api/me")
                .with(jwt().jwt(jwt -> {
                    jwt.claim("sub", "user-2");
                    jwt.claim("tenant_id", "tenant2");
                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant2"));
    }
}


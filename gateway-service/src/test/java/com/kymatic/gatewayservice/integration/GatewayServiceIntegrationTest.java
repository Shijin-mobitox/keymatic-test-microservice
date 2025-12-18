package com.kymatic.gatewayservice.integration;

import com.kymatic.shared.multitenancy.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Integration tests for gateway-service using Testcontainers (reactive).
 * 
 * This test suite:
 * - Starts Keycloak container with test realm
 * - Tests JWT authentication and tenant resolution in reactive context
 * - Validates tenant-aware endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class GatewayServiceIntegrationTest {

    @Autowired
    private org.springframework.test.web.reactive.server.WebTestClient webTestClient;

    // Keycloak container for authentication testing (aligned with production version)
    @Container
    static GenericContainer<?> keycloak = new GenericContainer<>(
            DockerImageName.parse("quay.io/keycloak/keycloak:26.2.0"))
            .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
            .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .withCommand("start-dev", "--import-realm")
            .withExposedPorts(8080)
            .withClasspathResourceMapping(
                    "realm-export.json",
                    "/opt/keycloak/data/import/realm-export.json",
                    org.testcontainers.containers.BindMode.READ_ONLY
            )
            .waitingFor(Wait.forHttp("/health").forPort(8080)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure Keycloak issuer URI
        String keycloakUrl = "http://localhost:" + keycloak.getMappedPort(8080);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloakUrl + "/realms/kymatic");
    }

    @BeforeEach
    void setUp() {
        // Clear TenantContext before each test
        TenantContext.clear();
    }

    @Test
    void testGetMeEndpointWithTenantId() {
        webTestClient
            .mutateWith(mockJwt().jwt(jwt -> {
                jwt.claim("sub", "user-123");
                jwt.claim("preferred_username", "testuser");
                jwt.claim("email", "testuser@example.com");
                jwt.claim("tenant_id", "tenant1");
            }))
            .get()
            .uri("/api/me")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.subject").isEqualTo("user-123")
            .jsonPath("$.username").isEqualTo("testuser")
            .jsonPath("$.email").isEqualTo("testuser@example.com")
            .jsonPath("$.tenantId").isEqualTo("tenant1")
            .jsonPath("$.tenantIdFromJwt").isEqualTo("tenant1")
            .jsonPath("$.service").isEqualTo("gateway-service");
    }

    @Test
    void testGetMeEndpointUnauthenticated() {
        webTestClient
            .get()
            .uri("/api/me")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void testGetCurrentTenantWithTenantId() {
        webTestClient
            .mutateWith(mockJwt().jwt(jwt -> {
                jwt.claim("tenant_id", "tenant1");
            }))
            .get()
            .uri("/api/tenant/current")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class).isEqualTo("Current tenant ID: tenant1");
    }

    @Test
    void testPublicEndpointsAreAccessible() {
        webTestClient
            .get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void testDifferentTenants() {
        // Test tenant1
        webTestClient
            .mutateWith(mockJwt().jwt(jwt -> {
                jwt.claim("sub", "user-1");
                jwt.claim("tenant_id", "tenant1");
            }))
            .get()
            .uri("/api/me")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.tenantId").isEqualTo("tenant1");

        // Test tenant2
        webTestClient
            .mutateWith(mockJwt().jwt(jwt -> {
                jwt.claim("sub", "user-2");
                jwt.claim("tenant_id", "tenant2");
            }))
            .get()
            .uri("/api/me")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.tenantId").isEqualTo("tenant2");
    }
}


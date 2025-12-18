# Build Fix - Testcontainers Keycloak Dependency

## Issue
Build was failing with error:
```
Could not find org.testcontainers:keycloak:1.19.7.
```

## Root Cause
Testcontainers doesn't provide a dedicated `keycloak` module. The dependency `org.testcontainers:keycloak:1.19.7` doesn't exist.

## Solution
Removed the non-existent `testcontainers:keycloak` dependency from all services. The integration tests use `GenericContainer` with the Keycloak Docker image, which is already provided by `testcontainers:junit-jupiter`.

## Files Fixed

### 1. `api-service/build.gradle`
**Before:**
```gradle
testImplementation 'org.testcontainers:keycloak:1.19.7'
```

**After:**
```gradle
// Note: Testcontainers doesn't have a dedicated Keycloak module
// We use GenericContainer with Keycloak image instead (see integration tests)
```

### 2. `auth-service/build.gradle`
**Before:**
```gradle
testImplementation 'org.testcontainers:keycloak'
```

**After:**
```gradle
// Note: Testcontainers doesn't have a dedicated Keycloak module
// We use GenericContainer with Keycloak image instead (see integration tests)
```

### 3. `gateway-service/build.gradle`
**Before:**
```gradle
testImplementation 'org.testcontainers:keycloak:1.19.7'
```

**After:**
```gradle
// Note: Testcontainers doesn't have a dedicated Keycloak module
// We use GenericContainer with Keycloak image instead (see integration tests)
```

### 4. `tenant-service/build.gradle`
**Before:**
```gradle
testImplementation 'org.testcontainers:keycloak:1.19.7'
```

**After:**
```gradle
// Note: Testcontainers doesn't have a dedicated Keycloak module
// We use GenericContainer with Keycloak image instead (see integration tests)
```

## How It Works

The integration tests use `GenericContainer` to run Keycloak (now aligned with production Keycloak 26.2.0):

```java
@Container
static GenericContainer<?> keycloak = new GenericContainer<>(
        DockerImageName.parse("quay.io/keycloak/keycloak:26.2.0"))
        .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
        .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
        .withCommand("start-dev", "--import-realm")
        .withExposedPorts(8080)
        .withClasspathResourceMapping(
                "realm-export.json",
                "/opt/keycloak/data/import/realm-export.json",
                org.testcontainers.containers.BindMode.READ_ONLY
        )
        .waitingFor(Wait.forHttp("/health").forPort(8080)
                .withStartupTimeout(Duration.ofMinutes(3)));
```

The `GenericContainer` class is provided by `testcontainers:junit-jupiter`, which is already included in all services.

## Verification

Build should now succeed:
```bash
./gradlew clean build -x test
```

Expected output:
```
BUILD SUCCESSFUL
```

## Related Files

- All integration tests use `GenericContainer` for Keycloak:
  - `api-service/src/test/java/.../ApiServiceIntegrationTest.java`
  - `auth-service/src/test/java/.../AuthServiceIntegrationTest.java`
  - `gateway-service/src/test/java/.../GatewayServiceIntegrationTest.java`
  - `tenant-service/src/test/java/.../KeycloakIntegrationTest.java`
  - `tenant-service/src/test/java/.../KeycloakTokenIntegrationTest.java`

## Date Fixed
2025-11-11


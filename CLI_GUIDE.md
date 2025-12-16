# CLI Guide - Running KyMaticServiceV1 Multi-Tenant Application

## 📋 Table of Contents
1. [Prerequisites](#prerequisites)
2. [Initial Setup](#initial-setup)
3. [Building the Project](#building-the-project)
4. [Running Services](#running-services)
5. [Testing the Application](#testing-the-application)
6. [Troubleshooting](#troubleshooting)
7. [Change Log with Comments](#change-log-with-comments)

---

## Prerequisites

### Required Software
```bash
# Check Java version (Java 21 required)
java -version

# Check Gradle version
./gradlew --version

# Check Docker version
docker --version

# Check Docker Compose version
docker-compose --version
```

**Expected Output:**
- Java: 21.x
- Gradle: 8.x+
- Docker: 20.x+
- Docker Compose: 2.x+

---

## Initial Setup

### 1. Clone and Navigate to Project
```bash
cd D:\WorkRoom\KyMaticServiceV1
```

### 2. Verify Project Structure
```bash
# List all services
ls -la

# Expected services:
# - api-service/
# - auth-service/
# - gateway-service/
# - tenant-service/
# - shared/
# - config/keycloak/realm-export.json
```

---

## Building the Project

### Build All Services (Skip Tests for Faster Build)
```bash
# Build all services without running tests
./gradlew build -x test

# Or on Windows PowerShell:
.\gradlew build -x test
```

**What this does:**
- Compiles all Java code across all services
- Packages JAR files for each service
- Skips test execution (use `./gradlew build` to run tests)

### Build Individual Service
```bash
# Build specific service
./gradlew :api-service:build
./gradlew :auth-service:build
./gradlew :gateway-service:build
./gradlew :tenant-service:build
```

### Clean and Rebuild
```bash
# Clean all build artifacts
./gradlew clean

# Clean and rebuild
./gradlew clean build -x test
```

---

## Running Services

### Option 1: Docker Compose (Recommended)

#### Start All Services
```bash
# Start all services in detached mode
docker-compose up -d

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f keycloak
docker-compose logs -f tenant-service
```

**What this starts:**
- Keycloak (port 8085) - Authentication server
- PostgreSQL Database (port 5432) - Shared PostgreSQL database for all services
- Gateway Service (port 8081) - API Gateway
- Auth Service (port 8082) - Authentication service
- Tenant Service (port 8083) - Multi-tenant handler
- API Service (port 8084) - Business API

#### Check Service Status
```bash
# List all running containers
docker-compose ps

# Check if Keycloak is ready (wait 30-60 seconds after startup)
curl http://localhost:8085/health

# Or in PowerShell:
Invoke-WebRequest -Uri http://localhost:8085/health
```

#### Stop All Services
```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes data)
docker-compose down -v
```

#### Restart Services
```bash
# Restart all services
docker-compose restart

# Restart specific service
docker-compose restart tenant-service
```

#### Rebuild and Restart
```bash
# Rebuild images and restart
docker-compose up -d --build
```

### Option 2: Run Services Individually (Development)

#### Start Infrastructure First
```bash
# Start only database and Keycloak
docker-compose up -d keycloak postgres-db
```

#### Run Tenant Service
```bash
cd tenant-service
./gradlew bootRun

# Or on Windows:
.\gradlew bootRun
```

#### Run API Service
```bash
cd api-service
./gradlew bootRun
```

#### Run Auth Service
```bash
cd auth-service
./gradlew bootRun
```

#### Run Gateway Service
```bash
cd gateway-service
./gradlew bootRun
```

---

## Testing the Application

### 1. Verify Keycloak is Running

```bash
# Check Keycloak health
curl http://localhost:8085/health

# Access Keycloak Admin Console
# Open browser: http://localhost:8085
# Username: admin
# Password: admin
```

### 2. Get Access Token

#### PowerShell Script
```powershell
# Get token for user1 (tenant1)
$body = @{
    grant_type = "password"
    client_id = "tenant-service"
    client_secret = "tenant-secret"
    username = "user1"
    password = "password"
}

$response = Invoke-RestMethod -Uri "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" `
    -Method Post `
    -ContentType "application/x-www-form-urlencoded" `
    -Body $body

$token = $response.access_token
Write-Host "Access Token: $token"
```

#### Bash Script
```bash
# Get token for user1 (tenant1)
TOKEN=$(curl -X POST http://localhost:8085/realms/kymatic/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=tenant-service" \
  -d "client_secret=tenant-secret" \
  -d "username=user1" \
  -d "password=password" | jq -r '.access_token')

echo "Access Token: $TOKEN"
```

### 3. Test Tenant Service

```powershell
# Test /api/me endpoint
Invoke-RestMethod -Uri "http://localhost:8083/api/me" `
    -Headers @{Authorization="Bearer $token"} | ConvertTo-Json
```

**Expected Response:**
```json
{
  "subject": "user-id",
  "username": "user1",
  "email": "user1@tenant1.com",
  "tenantId": "tenant1",
  "tenantIdFromJwt": "tenant1",
  "message": "This is tenant-specific data for tenant: tenant1",
  "service": "tenant-service"
}
```

### 4. Test API Service

```powershell
# Get token for api-service client
$token = (Invoke-RestMethod -Uri "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" `
    -Method Post -ContentType "application/x-www-form-urlencoded" `
    -Body @{grant_type="password";client_id="api-service";client_secret="api-secret";username="user1";password="password"}).access_token

# Test endpoint
Invoke-RestMethod -Uri "http://localhost:8084/api/me" `
    -Headers @{Authorization="Bearer $token"} | ConvertTo-Json
```

### 5. Test Auth Service

```powershell
# Test endpoint
Invoke-RestMethod -Uri "http://localhost:8082/api/me" `
    -Headers @{Authorization="Bearer $token"} | ConvertTo-Json
```

### 6. Test Gateway Service

```powershell
# Get token for gateway-service client
$token = (Invoke-RestMethod -Uri "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" `
    -Method Post -ContentType "application/x-www-form-urlencoded" `
    -Body @{grant_type="password";client_id="gateway-service";client_secret="gateway-secret";username="user1";password="password"}).access_token

# Test endpoint
Invoke-RestMethod -Uri "http://localhost:8081/api/me" `
    -Headers @{Authorization="Bearer $token"} | ConvertTo-Json
```

### 7. Test Different Tenants

```powershell
# Test tenant1 (user1)
$token1 = (Invoke-RestMethod -Uri "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" `
    -Method Post -ContentType "application/x-www-form-urlencoded" `
    -Body @{grant_type="password";client_id="tenant-service";client_secret="tenant-secret";username="user1";password="password"}).access_token

Invoke-RestMethod -Uri "http://localhost:8083/api/me" `
    -Headers @{Authorization="Bearer $token1"} | Select-Object tenantId

# Test tenant2 (user2)
$token2 = (Invoke-RestMethod -Uri "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" `
    -Method Post -ContentType "application/x-www-form-urlencoded" `
    -Body @{grant_type="password";client_id="tenant-service";client_secret="tenant-secret";username="user2";password="password"}).access_token

Invoke-RestMethod -Uri "http://localhost:8083/api/me" `
    -Headers @{Authorization="Bearer $token2"} | Select-Object tenantId
```

### 8. Health Checks

```powershell
# Check all service health endpoints
@("8081", "8082", "8083", "8084", "8085") | ForEach-Object {
    Write-Host "Checking port $_..."
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$_/actuator/health" -ErrorAction Stop
        Write-Host "✓ Port $_ is healthy" -ForegroundColor Green
    } catch {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:$_/health" -ErrorAction Stop
            Write-Host "✓ Port $_ is healthy" -ForegroundColor Green
        } catch {
            Write-Host "✗ Port $_ is not responding" -ForegroundColor Red
        }
    }
}
```

---

## Running Tests

### Run All Tests
```bash
# Run all tests (this will start Testcontainers)
./gradlew test

# Run tests for specific service
./gradlew :tenant-service:test
./gradlew :api-service:test
./gradlew :auth-service:test
./gradlew :gateway-service:test
```

### Run Integration Tests Only
```bash
# Run integration tests
./gradlew test --tests "*IntegrationTest"
```

**Note:** Integration tests use Testcontainers and will:
- Start Keycloak container
- Start PostgreSQL container
- Import realm configuration
- Run tests against real containers
- Clean up containers after tests

---

## Troubleshooting

### Issue: Port Already in Use

```powershell
# Check what's using a port
netstat -ano | findstr :8083

# Kill process using port (replace PID with actual process ID)
taskkill /PID <PID> /F
```

### Issue: Keycloak Not Starting

```bash
# Check Keycloak logs
docker-compose logs keycloak

# Wait longer (Keycloak takes 30-60 seconds to start)
# Check if it's ready:
curl http://localhost:8085/health
```

### Issue: Services Can't Connect to Keycloak

```bash
# Verify Keycloak is accessible from within Docker network
docker-compose exec tenant-service ping keycloak

# Check if Keycloak realm is imported
# Access: http://localhost:8085/admin
# Login with admin/admin
# Check if "kymatic" realm exists
```

### Issue: Database Connection Errors

```bash
# Check database logs
docker-compose logs postgres-db

# Verify database is running
docker-compose ps | grep postgres-db
```

### Issue: Build Failures

```bash
# Clean and rebuild
./gradlew clean build

# Check for dependency issues
./gradlew dependencies

# Clear Gradle cache (if needed)
./gradlew clean --refresh-dependencies
```

### Issue: JWT Validation Errors

```bash
# Verify Keycloak issuer URI is correct
# Check application.yml in each service:
# spring.security.oauth2.resourceserver.jwt.issuer-uri=http://keycloak:8080/realms/kymatic

# For local testing, use:
# http://localhost:8085/realms/kymatic
```

---

## Change Log with Comments

### 🔄 Changes Made to Project

#### 1. **docker-compose.yml** - Updated Keycloak Configuration
```yaml
# CHANGE: Added KC_BOOTSTRAP_ADMIN_USERNAME and KC_BOOTSTRAP_ADMIN_PASSWORD
# REASON: Newer Keycloak versions require bootstrap admin credentials
# IMPACT: Keycloak will use these credentials for initial admin setup
environment:
  KC_BOOTSTRAP_ADMIN_USERNAME: admin      # NEW: Bootstrap admin username
  KC_BOOTSTRAP_ADMIN_PASSWORD: admin      # NEW: Bootstrap admin password
  KEYCLOAK_ADMIN: admin                   # KEPT: Legacy compatibility
  KEYCLOAK_ADMIN_PASSWORD: admin          # KEPT: Legacy compatibility
```

**CLI Command to Apply:**
```bash
# Restart Keycloak to apply changes
docker-compose restart keycloak
```

---

#### 2. **api-service** - Complete Multi-Tenant Implementation

**Files Changed:**

##### a. `api-service/src/main/java/com/kymatic/apiservice/tenant/TenantResolver.java`
```java
// CHANGE: Updated to use shared TenantContext instead of local one
// REASON: All services should use the same TenantContext for consistency
// IMPACT: Tenant ID is now shared across all services using the shared module

// BEFORE: Used com.kymatic.apiservice.tenant.TenantContext
// AFTER: Uses com.kymatic.shared.multitenancy.TenantContext
import com.kymatic.shared.multitenancy.TenantContext;  // NEW: Shared module
```

**CLI Command to Verify:**
```bash
# Check if TenantContext is from shared module
grep -r "import.*TenantContext" api-service/src/main/java/
```

##### b. `api-service/src/main/java/com/kymatic/apiservice/config/SecurityConfig.java`
```java
// CHANGE: Added complete OAuth2 Resource Server configuration
// REASON: Service needs to validate JWT tokens from Keycloak
// IMPACT: All endpoints now require valid JWT tokens (except public ones)

// NEW: OAuth2 Resource Server configuration
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> {
        // JWT decoder auto-configured from application.yml
    })
)

// NEW: TenantResolver filter integration
.addFilterAfter(tenantResolver, UsernamePasswordAuthenticationFilter.class)
```

**CLI Command to Test:**
```bash
# Test security configuration
curl -X GET http://localhost:8084/api/me
# Should return 401 Unauthorized (no token)

curl -X GET http://localhost:8084/api/me \
  -H "Authorization: Bearer <token>"
# Should return 200 OK with tenant data
```

##### c. `api-service/src/main/resources/application.yml`
```yaml
# CHANGE: Added OAuth2 Resource Server configuration
# REASON: Service needs Keycloak issuer URI for JWT validation
# IMPACT: Service can now validate JWT tokens from Keycloak

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://keycloak:8080/realms/kymatic  # NEW
          jwk-set-uri: http://keycloak:8080/realms/kymatic/protocol/openid-connect/certs  # NEW

# CHANGE: Added Arconia multi-tenancy configuration
# REASON: Enable tenant-aware data access
# IMPACT: Database operations are automatically scoped to tenant

arconia:
  tenant:
    management:
      enabled: true  # NEW
```

**CLI Command to Verify:**
```bash
# Check application.yml configuration
cat api-service/src/main/resources/application.yml | grep -A 5 "oauth2"
```

##### d. `api-service/build.gradle`
```gradle
// CHANGE: Added test dependencies for integration tests
// REASON: Need Testcontainers and Spring Security Test support
// IMPACT: Can now run integration tests with Keycloak containers

testImplementation 'org.springframework.security:spring-security-test'  // NEW
// Note: Testcontainers doesn't have a dedicated Keycloak module
// We use GenericContainer with Keycloak image instead (see integration tests)
// GenericContainer is provided by testcontainers:junit-jupiter
```

**CLI Command to Run Tests:**
```bash
# Run integration tests
./gradlew :api-service:test --tests "*IntegrationTest"
```

##### e. `api-service/src/test/java/.../ApiServiceIntegrationTest.java`
```java
// CHANGE: Created new integration test file
// REASON: Need to test JWT authentication and tenant resolution
// IMPACT: Validates that service correctly extracts tenant_id from JWT tokens
```

**CLI Command to Run:**
```bash
./gradlew :api-service:test --tests "ApiServiceIntegrationTest"
```

---

#### 3. **auth-service** - Complete Multi-Tenant Implementation

**Files Changed:**

##### a. `auth-service/src/main/java/com/kymatic/authservice/multitenancy/JwtTenantResolver.java`
```java
// CHANGE: Created new JwtTenantResolver filter
// REASON: Service needs to extract tenant_id from JWT tokens
// IMPACT: Tenant ID is now available in TenantContext for all requests
```

**CLI Command to Verify:**
```bash
# Check if filter exists
ls -la auth-service/src/main/java/com/kymatic/authservice/multitenancy/
```

##### b. `auth-service/src/main/java/com/kymatic/authservice/config/SecurityConfig.java`
```java
// CHANGE: Added OAuth2 Resource Server and TenantResolver integration
// REASON: Service needs JWT validation and tenant extraction
// IMPACT: Service now validates tokens and extracts tenant_id
```

**CLI Command to Test:**
```bash
# Test auth service security
curl -X GET http://localhost:8082/api/me \
  -H "Authorization: Bearer <token>"
```

##### c. `auth-service/src/main/resources/application.yml`
```yaml
# CHANGE: Updated OAuth2 configuration with correct realm name
# REASON: Was using "your-realm", now uses "kymatic"
# IMPACT: Service can now validate tokens from correct Keycloak realm

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://keycloak:8080/realms/kymatic  # CHANGED: was "your-realm"
```

**CLI Command to Verify:**
```bash
# Check realm configuration
grep "issuer-uri" auth-service/src/main/resources/application.yml
```

##### d. `auth-service/build.gradle`
```gradle
// CHANGE: Added missing dependencies
// REASON: Service needs Spring Security, JPA, and Flyway
// IMPACT: Service can now use database and security features

implementation 'org.springframework.boot:spring-boot-starter-security'  // NEW
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'  // NEW
implementation 'org.flywaydb:flyway-core:9.22.3'                          // NEW
implementation 'org.postgresql:postgresql:42.7.2'                         // NEW
```

**CLI Command to Build:**
```bash
./gradlew :auth-service:build
```

---

#### 4. **gateway-service** - Reactive Multi-Tenant Implementation

**Files Changed:**

##### a. `gateway-service/src/main/java/com/kymatic/gatewayservice/config/ReactiveSecurityConfig.java`
```java
// CHANGE: Created reactive security configuration
// REASON: Gateway uses WebFlux (reactive), needs ServerHttpSecurity instead of HttpSecurity
// IMPACT: Service can now validate JWT tokens in reactive context

@EnableWebFluxSecurity  // NEW: Reactive security
public class ReactiveSecurityConfig {
    // Uses ServerHttpSecurity instead of HttpSecurity
}
```

**CLI Command to Verify:**
```bash
# Check reactive security config
ls -la gateway-service/src/main/java/com/kymatic/gatewayservice/config/
```

##### b. `gateway-service/src/main/java/com/kymatic/gatewayservice/multitenancy/ReactiveJwtTenantResolver.java`
```java
// CHANGE: Created reactive tenant resolver using WebFilter
// REASON: Gateway is reactive, needs WebFilter instead of OncePerRequestFilter
// IMPACT: Tenant ID extraction works in reactive context

// Uses ReactiveSecurityContextHolder instead of SecurityContextHolder
// Uses WebFilter instead of OncePerRequestFilter
```

**CLI Command to Test:**
```bash
# Test reactive tenant resolution
curl -X GET http://localhost:8081/api/me \
  -H "Authorization: Bearer <token>"
```

##### c. `gateway-service/src/main/resources/application.yml`
```yaml
# CHANGE: Added OAuth2 Resource Server configuration for reactive
# REASON: Gateway needs to validate JWT tokens
# IMPACT: Gateway can now validate tokens and extract tenant_id

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://keycloak:8080/realms/kymatic  # NEW
```

**CLI Command to Verify:**
```bash
# Check gateway configuration
cat gateway-service/src/main/resources/application.yml | grep -A 3 "oauth2"
```

##### d. `gateway-service/build.gradle`
```gradle
// CHANGE: Added OAuth2 Resource Server and Security dependencies
// REASON: Gateway needs JWT validation
// IMPACT: Gateway can now validate JWT tokens

implementation 'org.springframework.boot:spring-boot-starter-security'        // NEW
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'  // NEW
```

**CLI Command to Build:**
```bash
./gradlew :gateway-service:build
```

---

#### 5. **Shared Module** - No Changes
```java
// NOTE: Shared TenantContext already existed and is used by all services
// LOCATION: shared/src/main/java/com/kymatic/shared/multitenancy/TenantContext.java
// USAGE: All services now use this shared implementation
```

**CLI Command to Verify:**
```bash
# Check shared module
cat shared/src/main/java/com/kymatic/shared/multitenancy/TenantContext.java
```

---

## Quick Reference Commands

### Start Everything
```bash
# Build project
./gradlew build -x test

# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

### Stop Everything
```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes data)
docker-compose down -v
```

### Test Everything
```bash
# Run all tests
./gradlew test

# Run integration tests only
./gradlew test --tests "*IntegrationTest"
```

### Get Token and Test
```powershell
# Get token
$token = (Invoke-RestMethod -Uri "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" `
    -Method Post -ContentType "application/x-www-form-urlencoded" `
    -Body @{grant_type="password";client_id="tenant-service";client_secret="tenant-secret";username="user1";password="password"}).access_token

# Test endpoint
Invoke-RestMethod -Uri "http://localhost:8083/api/me" `
    -Headers @{Authorization="Bearer $token"} | ConvertTo-Json
```

---

## Summary of All Changes

| Service | Changes Made | CLI Command to Verify |
|---------|-------------|----------------------|
| **docker-compose.yml** | Added KC_BOOTSTRAP_ADMIN credentials | `grep KC_BOOTSTRAP docker-compose.yml` |
| **api-service** | Complete OAuth2 + TenantResolver + Tests | `./gradlew :api-service:test` |
| **auth-service** | Complete OAuth2 + TenantResolver + Tests | `./gradlew :auth-service:test` |
| **gateway-service** | Reactive OAuth2 + ReactiveTenantResolver + Tests | `./gradlew :gateway-service:test` |
| **tenant-service** | Already complete (no changes) | `./gradlew :tenant-service:test` |

---

**All services are now ready to run!** 🚀

For detailed information about each change, see the comments in the code files or refer to `PROJECT_IMPLEMENTATION_SUMMARY.md`.


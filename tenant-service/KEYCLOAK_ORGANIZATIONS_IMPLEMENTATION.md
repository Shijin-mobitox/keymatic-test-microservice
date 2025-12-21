# Keycloak Organizations Multi-Tenant Registration Implementation

This document describes the implementation of multi-tenant registration using Keycloak Organizations feature (Keycloak >= 26).

## Overview

The implementation provides a complete flow for tenant onboarding:
1. Create organization in Keycloak
2. Create and initialize tenant database
3. Create initial admin user(s) in Keycloak
4. Assign users to organization
5. Assign roles to users

## Architecture

### Components

1. **KeycloakClientWrapper** (`client/KeycloakClientWrapper.java`)
   - Wraps Keycloak Admin Client and REST API calls
   - Handles Organizations API via REST (since Admin Client may not fully support it in v26)
   - Handles User management via Admin Client
   - Provides rollback methods for cleanup

2. **TenantOnboardingService** (`service/TenantOnboardingService.java`)
   - Orchestrates the complete tenant onboarding flow
   - Integrates with existing `TenantDatabaseManager` for DB creation
   - Provides rollback/compensation logic for failed operations

3. **TenantController** (`controller/TenantController.java`)
   - REST endpoints for tenant and user creation
   - `POST /api/tenants` - Create tenant with Keycloak Organizations
   - `POST /api/tenants/{tenantAlias}/users` - Create user under tenant

4. **DTOs**
   - `CreateTenantRequest` - Request for tenant creation
   - `CreateUserRequest` - Request for user creation
   - `TenantOnboardingResponse` - Response for tenant creation
   - `UserOnboardingResponse` - Response for user creation

5. **Exception Classes**
   - `KeycloakException` - General Keycloak errors
   - `OrganizationAlreadyExistsException` - Duplicate organization
   - `UserAlreadyExistsException` - Duplicate user
   - `TenantOnboardingException` - Onboarding failures with step tracking

## Configuration

### application.yml

```yaml
keycloak:
  admin:
    server-url: ${KEYCLOAK_SERVER_URL:http://keycloak:8080}
    realm: ${KEYCLOAK_REALM:kymatic}
    client-id: ${KEYCLOAK_ADMIN_CLIENT_ID:admin-cli}
    client-secret: ${KEYCLOAK_ADMIN_CLIENT_SECRET:}
    username: ${KEYCLOAK_ADMIN_USERNAME:admin}
    password: ${KEYCLOAK_ADMIN_PASSWORD:admin}
    grant-type: ${KEYCLOAK_ADMIN_GRANT_TYPE:password}
    connection-pool-size: ${KEYCLOAK_ADMIN_POOL_SIZE:10}
```

### Dependencies (build.gradle)

```gradle
// Keycloak Admin Client for Organizations API (Keycloak >= 26)
implementation 'org.keycloak:keycloak-admin-client:26.0.0'
// Jakarta REST API for Keycloak Admin Client
implementation 'jakarta.ws.rs:jakarta.ws.rs-api:3.1.0'
implementation 'org.jboss.resteasy:resteasy-client:6.2.8.Final'
implementation 'org.jboss.resteasy:resteasy-jackson2-provider:6.2.8.Final'
```

## API Endpoints

### Create Tenant

**POST** `/api/tenants`

Creates a new tenant with Keycloak Organizations integration.

**Request Body:**
```json
{
  "tenantName": "Acme Corp",
  "slug": "acme",
  "subscriptionTier": "starter",
  "maxUsers": 100,
  "maxStorageGb": 50,
  "adminUser": {
    "email": "admin@acme.com",
    "password": "SecurePassword123!",
    "firstName": "John",
    "lastName": "Doe",
    "emailVerified": true
  },
  "metadata": {}
}
```

**Response (201 Created):**
```json
{
  "tenantId": "uuid",
  "tenantName": "Acme Corp",
  "slug": "acme",
  "keycloakOrganizationId": "org-id",
  "keycloakOrganizationAlias": "acme",
  "databaseName": "acme",
  "databaseConnectionString": "jdbc:postgresql://localhost:5432/acme",
  "status": "active",
  "adminUserId": "user-id",
  "adminUserEmail": "admin@acme.com",
  "createdAt": "2024-01-01T00:00:00Z",
  "metadata": {}
}
```

### Create User for Tenant

**POST** `/api/tenants/{tenantAlias}/users`

Creates a new user under a tenant (organization).

**Request Body:**
```json
{
  "email": "user@acme.com",
  "password": "SecurePassword123!",
  "firstName": "Jane",
  "lastName": "Smith",
  "emailVerified": true,
  "role": "user"
}
```

**Response (201 Created):**
```json
{
  "userId": "user-id",
  "email": "user@acme.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "organizationId": "org-id",
  "organizationAlias": "acme",
  "role": "user"
}
```

## Error Handling

### Error Responses

**409 Conflict** - Organization or user already exists
```json
{
  "message": "Organization with alias 'acme' already exists in Keycloak",
  "status": 409
}
```

**500 Internal Server Error** - Onboarding failure
```json
{
  "message": "Tenant onboarding failed at step 'DATABASE_CREATION' for tenant 'acme': ...",
  "tenantAlias": "acme",
  "failedStep": "DATABASE_CREATION",
  "status": 500
}
```

## Rollback/Compensation Logic

The `TenantOnboardingService` includes comprehensive rollback logic:

1. If organization creation fails → No cleanup needed
2. If database creation fails → Delete organization from Keycloak
3. If user creation fails → Delete organization and database (manual DB cleanup may be needed)
4. If user assignment fails → Delete user and organization

Rollback is performed in reverse order of creation to ensure clean state.

## Integration with Existing Code

### Database Creation

The implementation uses the existing `TenantDatabaseManager`:
- `createDatabaseIfNotExists(String databaseName)` - Creates tenant database
- `migrateTenantDatabase(String databaseName)` - Runs Flyway migrations

### Camunda Workflow Integration

To integrate with existing Camunda workflows, modify `TenantOnboardingService.createTenant()`:

```java
// After Step 4 (saving tenant record), trigger Camunda workflow
if (workflowService != null) {
    workflowService.startTenantProvisioningWorkflow(saved.getTenantId());
}
```

## Security Considerations

1. **Admin Credentials**: Store Keycloak admin credentials securely (use environment variables or secrets management)
2. **Service Account**: Consider using a dedicated service account with limited permissions instead of admin user
3. **Client Credentials Grant**: For production, use client credentials grant type instead of password grant
4. **HTTPS**: Always use HTTPS for Keycloak server URL in production

## Testing

### Unit Tests

Test individual components:
- `KeycloakClientWrapper` - Mock Keycloak responses
- `TenantOnboardingService` - Mock Keycloak and database operations

### Integration Tests

Test complete flow:
- Use Testcontainers for Keycloak and PostgreSQL
- Verify organization creation in Keycloak
- Verify database creation and migrations
- Verify user creation and assignment

## Notes

1. **Keycloak Version**: Requires Keycloak >= 26 for Organizations feature
2. **Organizations API**: Uses REST API calls as Admin Client may not fully support Organizations in v26
3. **Database Cleanup**: Failed database creation may leave orphaned databases - manual cleanup may be required
4. **Role Assignment**: Role assignment is optional and will not fail onboarding if role doesn't exist

## Future Enhancements

1. Support for inviting users via email
2. Support for domain-based organization assignment
3. Support for bulk user creation
4. Integration with Camunda workflows for async processing
5. Support for organization hierarchies


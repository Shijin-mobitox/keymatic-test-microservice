# Workflow-Service Orchestration Implementation

This document describes the implementation of tenant onboarding orchestration through workflow-service using Camunda BPMN workflows.

## Overview

The tenant onboarding process is now fully orchestrated by **workflow-service** using Camunda workflows. The workflow handles:
1. **Organization Creation** in Keycloak
2. **Database Creation** and migration
3. **User Creation** in Keycloak
4. **User Assignment** to organization
5. **Tenant Record** saving in tenant-service

## Architecture

### Workflow Steps

The `tenant-provisioning.bpmn` workflow includes the following service tasks:

1. **Create Organization in Keycloak** (`organizationCreationDelegate`)
   - Creates organization in Keycloak using Organizations API
   - Sets `keycloakOrganizationId` workflow variable

2. **Create Tenant Database** (`databaseCreationDelegate`)
   - Creates PostgreSQL database
   - Runs Flyway migrations
   - Sets `databaseName` and `databaseConnectionString` workflow variables

3. **Create Admin User in Keycloak** (`userCreationDelegate`)
   - Creates user in Keycloak
   - Sets `adminUserId` workflow variable

4. **Assign User to Organization** (`userAssignmentDelegate`)
   - Assigns user to organization
   - Assigns admin role to user (optional, non-blocking)

5. **Save Tenant Record** (`saveTenantRecordDelegate`)
   - Saves tenant record in tenant-service master database
   - Sets `tenantId` workflow variable

## Components

### Delegates

1. **OrganizationCreationDelegate**
   - Uses `KeycloakClientWrapper` to create organization
   - Requires: `slug`, `tenantName`
   - Sets: `keycloakOrganizationId`, `organizationCreated`

2. **DatabaseCreationDelegate**
   - Uses `DatabaseCreationService` to create and migrate database
   - Requires: `slug`
   - Sets: `databaseName`, `databaseConnectionString`, `migrationsApplied`, `databaseCreated`

3. **UserCreationDelegate**
   - Uses `KeycloakClientWrapper` to create user
   - Requires: `adminEmail`, `adminPassword`, `adminFirstName`, `adminLastName`, `adminEmailVerified`
   - Sets: `adminUserId`, `userCreated`

4. **UserAssignmentDelegate**
   - Uses `KeycloakClientWrapper` to assign user to organization
   - Requires: `keycloakOrganizationId`, `adminUserId`, `adminRole`
   - Sets: `userAssigned`

5. **SaveTenantRecordDelegate**
   - Calls tenant-service API to save tenant record
   - Requires: All tenant information from previous steps
   - Sets: `tenantId`, `tenantRecordSaved`

### Services

1. **KeycloakClientWrapper**
   - Wraps Keycloak Admin Client and REST API
   - Handles Organizations API via REST
   - Handles User management via Admin Client

2. **DatabaseCreationService**
   - Creates PostgreSQL databases
   - Runs Flyway migrations
   - Manages database connections

3. **TenantWorkflowService**
   - Starts Camunda workflows
   - Maps request DTOs to workflow variables

## API Endpoint

### Start Tenant Provisioning Workflow

**POST** `/api/workflows/tenants/provision`

**Request Body:**
```json
{
  "tenantName": "Acme Corp",
  "slug": "acme",
  "subscriptionTier": "starter",
  "maxUsers": 100,
  "maxStorageGb": 50,
  "adminEmail": "admin@acme.com",
  "adminPassword": "SecurePassword123!",
  "adminFirstName": "John",
  "adminLastName": "Doe",
  "adminEmailVerified": true,
  "metadata": {}
}
```

**Response (202 Accepted):**
```json
{
  "processInstanceId": "process-instance-id",
  "status": "started"
}
```

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

tenant-database:
  base-url: ${TENANT_DB_BASE_URL:jdbc:postgresql://localhost:5432}
  username: ${TENANT_DB_USERNAME:postgres}
  password: ${TENANT_DB_PASSWORD:root}
  migration-locations: classpath:db/tenant
```

## Workflow Variables

### Input Variables (from API request)
- `tenantName` - Tenant display name
- `slug` - Tenant slug (URL-friendly)
- `subscriptionTier` - Subscription tier
- `maxUsers` - Maximum users
- `maxStorageGb` - Maximum storage GB
- `adminEmail` - Admin user email
- `adminPassword` - Admin user password
- `adminFirstName` - Admin user first name
- `adminLastName` - Admin user last name
- `adminEmailVerified` - Whether email is verified
- `adminRole` - Role to assign (defaults to "admin")
- `metadata` - Optional metadata JSON

### Output Variables (set by workflow steps)
- `keycloakOrganizationId` - Created organization ID
- `organizationCreated` - Boolean flag
- `databaseName` - Created database name
- `databaseConnectionString` - JDBC connection string
- `migrationsApplied` - List of applied migration versions
- `databaseCreated` - Boolean flag
- `adminUserId` - Created user ID
- `userCreated` - Boolean flag
- `userAssigned` - Boolean flag
- `tenantId` - Saved tenant ID
- `tenantRecordSaved` - Boolean flag

## Error Handling

If any step fails:
- The workflow will stop at that step
- Previous steps are not automatically rolled back
- Manual cleanup may be required for:
  - Keycloak organizations
  - Keycloak users
  - PostgreSQL databases

**Future Enhancement**: Add error boundary events and compensation handlers for automatic rollback.

## Integration with Tenant-Service

The `SaveTenantRecordDelegate` calls tenant-service to save the tenant record. 

**Note**: Currently, it uses the legacy `/api/tenants` endpoint which may attempt to create the database again. The endpoint should handle the case where the database already exists gracefully.

**Recommended**: Create a new endpoint in tenant-service specifically for saving tenant records without database creation, or modify the existing endpoint to check if the database exists first.

## Benefits of Workflow Orchestration

1. **Visibility**: Full visibility into the onboarding process through Camunda Cockpit
2. **Retry Logic**: Can implement retry logic for failed steps
3. **Compensation**: Can add compensation handlers for rollback
4. **Monitoring**: Track workflow execution metrics
5. **Scalability**: Workflows can be executed asynchronously
6. **Flexibility**: Easy to add new steps or modify the flow

## Testing

### Unit Tests
- Test each delegate independently with mocked dependencies
- Test workflow variable mapping in `TenantWorkflowService`

### Integration Tests
- Test complete workflow execution with Testcontainers
- Verify all steps complete successfully
- Test error scenarios and rollback

## Dependencies

- **Keycloak Admin Client** 26.0.0
- **Flyway** 9.22.3
- **Camunda BPM** 7.21.0
- **PostgreSQL JDBC Driver** 42.7.2

## Future Enhancements

1. Add error boundary events for automatic error handling
2. Add compensation handlers for automatic rollback
3. Add retry logic for transient failures
4. Add notification steps (email, webhook)
5. Add approval steps for tenant creation
6. Support for tenant update workflows
7. Support for tenant deletion workflows


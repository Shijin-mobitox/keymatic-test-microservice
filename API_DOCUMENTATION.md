# API Documentation

Complete CRUD API documentation for KyMatic Multi-Tenant Service.

**Base URL:** `http://localhost:8083` (tenant-service)

**Authentication:** All endpoints require JWT Bearer token in the `Authorization` header:
```
Authorization: Bearer <your-jwt-token>
```

---

## Table of Contents

1. [Master Database APIs](#master-database-apis)
   - [Tenants](#tenants)
   - [Tenant Users](#tenant-users)
   - [Subscriptions](#subscriptions)
   - [Audit Logs](#audit-logs)
   - [Tenant Migrations](#tenant-migrations)

2. [Tenant-Specific APIs](#tenant-specific-apis)
   - [Users](#users)
   - [Projects](#projects)
   - [Tasks](#tasks)
   - [Activity Logs](#activity-logs)

---

## Master Database APIs

### Tenants

#### Create Tenant
```http
POST /api/tenants
Content-Type: application/json
```

**Request Body:**
```json
{
  "tenantName": "Acme Corporation",
  "slug": "acme",
  "subscriptionTier": "premium",
  "maxUsers": 100,
  "maxStorageGb": 50,
  "adminEmail": "admin@acme.com",
  "metadata": {
    "industry": "Technology",
    "region": "US"
  }
}
```

**Response:** `201 Created`
```json
{
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "tenantName": "Acme Corporation",
  "slug": "acme",
  "status": "active",
  "subscriptionTier": "premium",
  "databaseConnectionString": "jdbc:postgresql://localhost:5432/tenant_acme",
  "databaseName": "tenant_acme",
  "maxUsers": 100,
  "maxStorageGb": 50,
  "createdAt": "2025-01-15T10:30:00Z",
  "updatedAt": "2025-01-15T10:30:00Z",
  "metadata": {
    "industry": "Technology",
    "region": "US"
  }
}
```

#### List Tenants
```http
GET /api/tenants?page=0&size=10
```

**Response:** `200 OK`
```json
[
  {
    "tenantId": "550e8400-e29b-41d4-a716-446655440000",
    "tenantName": "Acme Corporation",
    "slug": "acme",
    "status": "active",
    ...
  }
]
```

#### Get Tenant by ID
```http
GET /api/tenants/{tenantId}
```

#### Get Tenant by Slug
```http
GET /api/tenants/slug/{slug}
```

#### Update Tenant Status
```http
POST /api/tenants/{tenantId}/status
Content-Type: application/json
```

**Request Body:**
```json
{
  "status": "suspended"
}
```

**Status values:** `active`, `suspended`, `deleted`

---

### Tenant Users

#### Create Tenant User
```http
POST /api/tenant-users
Content-Type: application/json
```

**Request Body:**
```json
{
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@acme.com",
  "password": "SecurePassword123!",
  "role": "admin",
  "isActive": true
}
```

**Response:** `201 Created`
```json
{
  "userId": "660e8400-e29b-41d4-a716-446655440001",
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@acme.com",
  "role": "admin",
  "isActive": true,
  "lastLogin": null,
  "createdAt": "2025-01-15T10:35:00Z"
}
```

#### Get Tenant User by ID
```http
GET /api/tenant-users/{userId}
```

#### Get Tenant User by Email
```http
GET /api/tenant-users/tenant/{tenantId}/email?email=user@acme.com
```

#### List Users by Tenant
```http
GET /api/tenant-users/tenant/{tenantId}
```

#### Update Tenant User
```http
PUT /api/tenant-users/{userId}
Content-Type: application/json
```

**Request Body:**
```json
{
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@acme.com",
  "password": "NewPassword123!",
  "role": "user",
  "isActive": true
}
```

#### Delete Tenant User
```http
DELETE /api/tenant-users/{userId}
```

**Response:** `204 No Content`

---

### Subscriptions

#### Create Subscription
```http
POST /api/subscriptions
Content-Type: application/json
```

**Request Body:**
```json
{
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "planName": "Premium Plan",
  "billingCycle": "monthly",
  "amount": 99.99,
  "currency": "USD",
  "status": "active",
  "currentPeriodStart": "2025-01-01",
  "currentPeriodEnd": "2025-02-01"
}
```

**Response:** `201 Created`
```json
{
  "subscriptionId": "770e8400-e29b-41d4-a716-446655440002",
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "planName": "Premium Plan",
  "billingCycle": "monthly",
  "amount": 99.99,
  "currency": "USD",
  "status": "active",
  "currentPeriodStart": "2025-01-01",
  "currentPeriodEnd": "2025-02-01",
  "createdAt": "2025-01-15T10:40:00Z"
}
```

#### Get Subscription by ID
```http
GET /api/subscriptions/{subscriptionId}
```

#### List Subscriptions by Tenant
```http
GET /api/subscriptions/tenant/{tenantId}
```

#### Update Subscription
```http
PUT /api/subscriptions/{subscriptionId}
Content-Type: application/json
```

**Request Body:** (same as create)

#### Delete Subscription
```http
DELETE /api/subscriptions/{subscriptionId}
```

**Response:** `204 No Content`

---

### Audit Logs

#### Get Audit Log by ID
```http
GET /api/audit-logs/{logId}
```

**Response:** `200 OK`
```json
{
  "logId": 1,
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "action": "user.created",
  "performedBy": "660e8400-e29b-41d4-a716-446655440001",
  "details": {
    "userId": "660e8400-e29b-41d4-a716-446655440001",
    "email": "user@acme.com"
  },
  "createdAt": "2025-01-15T10:35:00Z"
}
```

#### List Audit Logs by Tenant
```http
GET /api/audit-logs/tenant/{tenantId}?page=0&size=20
```

#### List Audit Logs by Action
```http
GET /api/audit-logs/tenant/{tenantId}/action/{action}
```

**Example:** `GET /api/audit-logs/tenant/{tenantId}/action/user.created`

---

### Tenant Migrations

#### List Tenant Migrations
```http
GET /api/tenants/{tenantId}/migrations
```

**Response:** `200 OK`
```json
[
  {
    "migrationId": 1,
    "tenantId": "550e8400-e29b-41d4-a716-446655440000",
    "version": "V1",
    "status": "success",
    "appliedAt": "2025-01-15T10:30:00Z"
  }
]
```

#### Run Tenant Migrations
```http
POST /api/tenants/{tenantId}/migrations/run
```

**Response:** `202 Accepted`

---

## Tenant-Specific APIs

These APIs automatically use the tenant ID from the JWT token. No need to specify tenant ID in the request.

### Users

#### Create User
```http
POST /api/users
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "john.doe@acme.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "developer",
  "isActive": true
}
```

**Response:** `201 Created`
```json
{
  "userId": "880e8400-e29b-41d4-a716-446655440003",
  "email": "john.doe@acme.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "developer",
  "isActive": true,
  "createdAt": "2025-01-15T11:00:00Z",
  "updatedAt": "2025-01-15T11:00:00Z"
}
```

#### List All Users
```http
GET /api/users
```

#### Get User by ID
```http
GET /api/users/{userId}
```

#### Get User by Email
```http
GET /api/users/email?email=john.doe@acme.com
```

#### Update User
```http
PUT /api/users/{userId}
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "john.doe@acme.com",
  "firstName": "John",
  "lastName": "Smith",
  "role": "senior-developer",
  "isActive": true
}
```

#### Delete User
```http
DELETE /api/users/{userId}
```

**Response:** `204 No Content`

---

### Projects

#### Create Project
```http
POST /api/projects
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Website Redesign",
  "description": "Complete redesign of company website",
  "ownerId": "880e8400-e29b-41d4-a716-446655440003",
  "status": "in-progress"
}
```

**Response:** `201 Created`
```json
{
  "projectId": "990e8400-e29b-41d4-a716-446655440004",
  "name": "Website Redesign",
  "description": "Complete redesign of company website",
  "ownerId": "880e8400-e29b-41d4-a716-446655440003",
  "status": "in-progress",
  "createdAt": "2025-01-15T11:10:00Z",
  "updatedAt": "2025-01-15T11:10:00Z"
}
```

#### List All Projects
```http
GET /api/projects
```

#### Get Project by ID
```http
GET /api/projects/{projectId}
```

#### List Projects by Owner
```http
GET /api/projects/owner/{ownerId}
```

#### Update Project
```http
PUT /api/projects/{projectId}
Content-Type: application/json
```

**Request Body:** (same as create)

#### Delete Project
```http
DELETE /api/projects/{projectId}
```

**Response:** `204 No Content`

---

### Tasks

#### Create Task
```http
POST /api/tasks
Content-Type: application/json
```

**Request Body:**
```json
{
  "projectId": "990e8400-e29b-41d4-a716-446655440004",
  "title": "Design homepage mockup",
  "description": "Create initial design mockup for homepage",
  "assignedTo": "880e8400-e29b-41d4-a716-446655440003",
  "status": "todo",
  "dueDate": "2025-01-25"
}
```

**Response:** `201 Created`
```json
{
  "taskId": "aa0e8400-e29b-41d4-a716-446655440005",
  "projectId": "990e8400-e29b-41d4-a716-446655440004",
  "title": "Design homepage mockup",
  "description": "Create initial design mockup for homepage",
  "assignedTo": "880e8400-e29b-41d4-a716-446655440003",
  "status": "todo",
  "dueDate": "2025-01-25",
  "createdAt": "2025-01-15T11:20:00Z",
  "updatedAt": "2025-01-15T11:20:00Z"
}
```

#### List All Tasks
```http
GET /api/tasks
```

#### Get Task by ID
```http
GET /api/tasks/{taskId}
```

#### List Tasks by Project
```http
GET /api/tasks/project/{projectId}
```

#### List Tasks by Assignee
```http
GET /api/tasks/assignee/{assignedTo}
```

#### Update Task
```http
PUT /api/tasks/{taskId}
Content-Type: application/json
```

**Request Body:** (same as create)

#### Delete Task
```http
DELETE /api/tasks/{taskId}
```

**Response:** `204 No Content`

---

### Activity Logs

#### List All Activity Logs
```http
GET /api/activity-logs?page=0&size=20
```

**Response:** `200 OK`
```json
[
  {
    "logId": 1,
    "userId": "880e8400-e29b-41d4-a716-446655440003",
    "action": "project.created",
    "entityType": "project",
    "entityId": "990e8400-e29b-41d4-a716-446655440004",
    "changes": {
      "name": "Website Redesign"
    },
    "createdAt": "2025-01-15T11:10:00Z"
  }
]
```

#### Get Activity Log by ID
```http
GET /api/activity-logs/{logId}
```

#### List Activity Logs by User
```http
GET /api/activity-logs/user/{userId}
```

#### List Activity Logs by Action
```http
GET /api/activity-logs/action/{action}
```

**Example:** `GET /api/activity-logs/action/project.created`

#### List Activity Logs by Entity
```http
GET /api/activity-logs/entity/{entityType}/{entityId}
```

**Example:** `GET /api/activity-logs/entity/project/990e8400-e29b-41d4-a716-446655440004`

---

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request
```json
{
  "timestamp": "2025-01-15T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/users"
}
```

### 401 Unauthorized
```json
{
  "timestamp": "2025-01-15T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token is missing or invalid",
  "path": "/api/users"
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-01-15T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "User not found: 880e8400-e29b-41d4-a716-446655440003",
  "path": "/api/users/880e8400-e29b-41d4-a716-446655440003"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2025-01-15T12:00:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/users"
}
```

---

## Testing Examples

### Using cURL

#### Get JWT Token (from Keycloak)
```bash
curl -X POST "http://localhost:8085/realms/kymatic/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=react-client" \
  -d "username=your-username" \
  -d "password=your-password"
```

#### Create a User
```bash
curl -X POST "http://localhost:8083/api/users" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "role": "developer",
    "isActive": true
  }'
```

#### List All Users
```bash
curl -X GET "http://localhost:8083/api/users" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Update a User
```bash
curl -X PUT "http://localhost:8083/api/users/{userId}" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "firstName": "Updated",
    "lastName": "Name",
    "role": "senior-developer",
    "isActive": true
  }'
```

#### Delete a User
```bash
curl -X DELETE "http://localhost:8083/api/users/{userId}" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Swagger/OpenAPI Documentation

Interactive API documentation is available at:
- **Swagger UI:** `http://localhost:8083/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8083/v3/api-docs`

---

## Notes

1. **Tenant Isolation:** Tenant-specific APIs (`/api/users`, `/api/projects`, `/api/tasks`, `/api/activity-logs`) automatically filter data by the tenant ID extracted from the JWT token.

2. **Master Database APIs:** APIs for `tenants`, `tenant-users`, `subscriptions`, and `audit-logs` operate on the master database and require explicit tenant ID in requests.

3. **Pagination:** List endpoints support pagination with `page` and `size` query parameters (default: page=0, size=10 or 20).

4. **Validation:** All request bodies are validated. Invalid requests return `400 Bad Request` with validation error details.

5. **Authentication:** All endpoints require a valid JWT token from Keycloak. The token must include a `tenant_id` claim for tenant-specific operations.


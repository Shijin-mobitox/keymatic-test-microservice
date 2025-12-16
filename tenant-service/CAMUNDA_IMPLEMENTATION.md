# Camunda Workflow Integration - Architecture Summary

## Overview

Camunda BPM now runs inside the dedicated `workflow-service` microservice (`workflow-service/`). Tenant-service still exposes the `/api/tenants/.../workflow` endpoints, but it delegates to workflow-service via Feign. This keeps the public API stable while letting the workflow engine scale and deploy independently.

## Components

### workflow-service (new microservice)
- **Dependencies**: Spring Boot 3.2.5 + Camunda Spring Boot starters + REST + Webapp.
- **Application**: `WorkflowServiceApplication` with `@EnableProcessApplication`.
- **Controller**: `WorkflowController` exposes
  - `POST /api/workflows/tenants/provision`
  - `POST /api/workflows/tenants/{tenantId}/status`
- **Service layer**: `TenantWorkflowService` starts BPMN processes; delegates invoke tenant-service via REST.
- **Delegates**:
  - `TenantProvisioningDelegate` & `TenantStatusUpdateDelegate` call tenant-service REST endpoints (`/api/tenants` & `/api/tenants/{id}/status`).
- **Configuration**:
  - `application.yml` configures datasource, Camunda admin user, deployment patterns, and `workflow.tenant-service.base-url`.
  - `WorkflowProperties` + `TenantServiceClient` wrap outbound REST calls.
- **BPMN definitions**: `tenant-provisioning.bpmn`, `tenant-status-update.bpmn`.
- **Docker**: `workflow-service/Dockerfile`; service added to `docker-compose.yml` on port `8090`.

### tenant-service (updated)
- Removed Camunda dependencies, delegates, BPMN files, and config.
- Added OpenFeign support (`@EnableFeignClients`).
- `WorkflowServiceClient` + `WorkflowOrchestrationService` forward workflow requests to workflow-service.
- `TenantController` still exposes:
  - `POST /api/tenants/workflow`
  - `POST /api/tenants/{tenantId}/status/workflow`
  but now these endpoints simply call workflow-service and return the resulting process-instance metadata.
- `application.yml` now holds `workflow.service.base-url` (defaults to `http://localhost:8090` or can be overridden via `WORKFLOW_SERVICE_URL`).

## Request flow
1. Client calls tenant-service workflow endpoint (no change for consumers).
2. Tenant-service forwards the request to workflow-service via Feign.
3. workflow-service starts the Camunda process.
4. Camunda delegates call back into tenant-service REST endpoints to perform provisioning/status updates.
5. workflow-service returns `{processInstanceId, status}` to tenant-service, which relays it to the client.

## Operations & Monitoring
- Run both services (tenant-service on 8083, workflow-service on 8090). docker-compose now includes workflow-service.
- Camunda Cockpit/REST API are hosted by workflow-service at `http://localhost:8090/camunda/app/cockpit`.
- Configure cross-service URLs via:
  - `WORKFLOW_SERVICE_URL` (tenant-service → workflow-service)
  - `TENANT_SERVICE_URL` (workflow-service → tenant-service)

## Benefits
- **Clear separation**: tenant-service focuses on business logic; workflow-service manages orchestration.
- **Scalability**: Camunda engine can scale independently.
- **Reusability**: Future microservices can call workflow-service without embedding Camunda.
- **Backward compatibility**: No change to tenant-service API; only internal routing changed.


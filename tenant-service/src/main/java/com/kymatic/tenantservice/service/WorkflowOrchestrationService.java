package com.kymatic.tenantservice.service;

import com.kymatic.tenantservice.client.WorkflowServiceClient;
import com.kymatic.tenantservice.dto.TenantRequest;
import com.kymatic.tenantservice.dto.TenantStatusUpdateRequest;
import com.kymatic.tenantservice.dto.workflow.WorkflowProcessResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WorkflowOrchestrationService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowOrchestrationService.class);

    private final WorkflowServiceClient workflowServiceClient;

    public WorkflowOrchestrationService(WorkflowServiceClient workflowServiceClient) {
        this.workflowServiceClient = workflowServiceClient;
    }

    public WorkflowProcessResponse startTenantProvisioning(TenantRequest request) {
        logger.info("Delegating tenant provisioning workflow to workflow-service for slug {}", request.slug());
        return workflowServiceClient.startTenantProvisioning(request);
    }

    public WorkflowProcessResponse startTenantStatusUpdate(UUID tenantId, TenantStatusUpdateRequest request) {
        logger.info("Delegating tenant status update workflow to workflow-service for tenant {}", tenantId);
        return workflowServiceClient.startTenantStatusUpdate(tenantId, request);
    }
}


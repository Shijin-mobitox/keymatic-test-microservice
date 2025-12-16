package com.kymatic.workflow.service;

import com.kymatic.workflow.dto.TenantRequest;
import com.kymatic.workflow.dto.TenantStatusUpdateRequest;
import com.kymatic.workflow.dto.WorkflowProcessResponse;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TenantWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(TenantWorkflowService.class);

    private final RuntimeService runtimeService;

    public TenantWorkflowService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public WorkflowProcessResponse startTenantProvisioning(TenantRequest request) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("tenantName", request.tenantName());
        variables.put("slug", request.slug());
        variables.put("subscriptionTier", request.subscriptionTier());
        variables.put("maxUsers", request.maxUsers());
        variables.put("maxStorageGb", request.maxStorageGb());
        variables.put("adminEmail", request.adminEmail());
        variables.put("metadata", request.metadata());

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("tenant-provisioning", variables);
        logger.info("Started tenant provisioning workflow instance {}", instance.getId());
        return WorkflowProcessResponse.started(instance.getId());
    }

    public WorkflowProcessResponse startTenantStatusUpdate(UUID tenantId, TenantStatusUpdateRequest request) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("tenantId", tenantId.toString());
        variables.put("status", request.status());

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("tenant-status-update", variables);
        logger.info("Started tenant status update workflow instance {}", instance.getId());
        return WorkflowProcessResponse.started(instance.getId());
    }
}


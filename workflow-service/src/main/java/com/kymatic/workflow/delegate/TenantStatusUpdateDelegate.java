package com.kymatic.workflow.delegate;

import com.kymatic.workflow.dto.TenantStatusUpdateRequest;
import com.kymatic.workflow.service.TenantServiceClient;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("tenantStatusUpdateDelegate")
public class TenantStatusUpdateDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(TenantStatusUpdateDelegate.class);

    private final TenantServiceClient tenantServiceClient;

    public TenantStatusUpdateDelegate(TenantServiceClient tenantServiceClient) {
        this.tenantServiceClient = tenantServiceClient;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String tenantId = (String) execution.getVariable("tenantId");
        String status = (String) execution.getVariable("status");

        tenantServiceClient.updateTenantStatus(
            UUID.fromString(tenantId),
            new TenantStatusUpdateRequest(status)
        );

        execution.setVariable("statusUpdated", true);
        logger.info("Tenant status update delegate completed for tenant {}", tenantId);
    }
}


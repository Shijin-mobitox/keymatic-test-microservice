package com.kymatic.workflow.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kymatic.workflow.dto.TenantRequest;
import com.kymatic.workflow.dto.TenantResponse;
import com.kymatic.workflow.service.TenantServiceClient;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("tenantProvisioningDelegate")
public class TenantProvisioningDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(TenantProvisioningDelegate.class);

    private final TenantServiceClient tenantServiceClient;
    private final ObjectMapper objectMapper;

    public TenantProvisioningDelegate(TenantServiceClient tenantServiceClient, ObjectMapper objectMapper) {
        this.tenantServiceClient = tenantServiceClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String tenantName = (String) execution.getVariable("tenantName");
        String slug = (String) execution.getVariable("slug");
        String subscriptionTier = (String) execution.getVariable("subscriptionTier");
        Integer maxUsers = (Integer) execution.getVariable("maxUsers");
        Integer maxStorageGb = (Integer) execution.getVariable("maxStorageGb");
        String adminEmail = (String) execution.getVariable("adminEmail");
        String adminPassword = (String) execution.getVariable("adminPassword");
        String adminFirstName = (String) execution.getVariable("adminFirstName");
        String adminLastName = (String) execution.getVariable("adminLastName");
        Boolean adminEmailVerified = (Boolean) execution.getVariable("adminEmailVerified");
        Object metadata = execution.getVariable("metadata");

        TenantRequest request = new TenantRequest(
            tenantName,
            slug,
            subscriptionTier,
            maxUsers,
            maxStorageGb,
            adminEmail != null ? adminEmail : "admin@" + slug + ".com", // adminEmail - default if not provided
            adminPassword != null ? adminPassword : "TempPassword123!", // adminPassword - default if not provided
            adminFirstName != null ? adminFirstName : "Admin", // adminFirstName - default if not provided
            adminLastName != null ? adminLastName : "User", // adminLastName - default if not provided
            adminEmailVerified != null ? adminEmailVerified : false, // adminEmailVerified
            metadata == null ? null : objectMapper.valueToTree(metadata)
        );

        TenantResponse response = tenantServiceClient.createTenant(request);
        execution.setVariable("tenantId", response.tenantId().toString());
        execution.setVariable("tenantCreated", true);
        execution.setVariable("databaseName", response.databaseName());

        logger.info("Tenant provisioning delegate created tenant {} ({})", response.tenantId(), response.slug());
    }
}


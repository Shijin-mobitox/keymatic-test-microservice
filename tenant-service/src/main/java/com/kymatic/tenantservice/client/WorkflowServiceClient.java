package com.kymatic.tenantservice.client;

import com.kymatic.tenantservice.dto.TenantRequest;
import com.kymatic.tenantservice.dto.TenantStatusUpdateRequest;
import com.kymatic.tenantservice.dto.workflow.WorkflowProcessResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
    name = "workflowServiceClient",
    url = "${workflow.service.base-url:http://localhost:8090}"
)
public interface WorkflowServiceClient {

    @PostMapping("/api/workflows/tenants/provision")
    WorkflowProcessResponse startTenantProvisioning(@RequestBody TenantRequest request);

    @PostMapping("/api/workflows/tenants/{tenantId}/status")
    WorkflowProcessResponse startTenantStatusUpdate(
        @PathVariable UUID tenantId,
        @RequestBody TenantStatusUpdateRequest request
    );
}


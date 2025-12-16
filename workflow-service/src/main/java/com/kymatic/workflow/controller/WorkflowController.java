package com.kymatic.workflow.controller;

import com.kymatic.workflow.dto.TenantRequest;
import com.kymatic.workflow.dto.TenantStatusUpdateRequest;
import com.kymatic.workflow.dto.WorkflowProcessResponse;
import com.kymatic.workflow.service.TenantWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/workflows/tenants", produces = MediaType.APPLICATION_JSON_VALUE)
public class WorkflowController {

    private final TenantWorkflowService tenantWorkflowService;

    public WorkflowController(TenantWorkflowService tenantWorkflowService) {
        this.tenantWorkflowService = tenantWorkflowService;
    }

    @PostMapping(value = "/provision", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> startTenantProvisioning(
        @Valid @RequestBody TenantRequest request
    ) {
        WorkflowProcessResponse response = tenantWorkflowService.startTenantProvisioning(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(Map.of(
                "processInstanceId", response.processInstanceId(),
                "status", response.status()
            ));
    }

    @PostMapping(value = "/{tenantId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> startTenantStatusUpdate(
        @PathVariable UUID tenantId,
        @Valid @RequestBody TenantStatusUpdateRequest request
    ) {
        WorkflowProcessResponse response = tenantWorkflowService.startTenantStatusUpdate(tenantId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(Map.of(
                "processInstanceId", response.processInstanceId(),
                "status", response.status()
            ));
    }
}


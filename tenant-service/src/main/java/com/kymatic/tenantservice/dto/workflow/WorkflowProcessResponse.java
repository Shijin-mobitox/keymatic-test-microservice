package com.kymatic.tenantservice.dto.workflow;

public record WorkflowProcessResponse(
    String processInstanceId,
    String status
) {}


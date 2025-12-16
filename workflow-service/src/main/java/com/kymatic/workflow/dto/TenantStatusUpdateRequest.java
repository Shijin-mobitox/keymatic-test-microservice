package com.kymatic.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record TenantStatusUpdateRequest(
    @NotBlank
    String status
) {}


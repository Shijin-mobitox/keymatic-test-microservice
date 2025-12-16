package com.kymatic.tenantservice.dto.rbac;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePermissionRequest(
    @NotBlank @Size(max = 100) String permissionKey,
    @NotBlank @Size(max = 255) String permissionName,
    String description,
    @NotBlank String category,
    @NotBlank String resource,
    @NotBlank String action
) {}


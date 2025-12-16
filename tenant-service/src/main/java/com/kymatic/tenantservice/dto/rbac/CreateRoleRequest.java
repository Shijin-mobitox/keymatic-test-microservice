package com.kymatic.tenantservice.dto.rbac;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateRoleRequest(
    @NotBlank @Size(max = 100) String roleName,
    @NotBlank @Size(max = 50) String roleKey,
    String description,
    @Min(10) @Max(100) Integer level,
    Boolean systemRole,
    @NotEmpty Set<@NotBlank String> permissionKeys
) {}


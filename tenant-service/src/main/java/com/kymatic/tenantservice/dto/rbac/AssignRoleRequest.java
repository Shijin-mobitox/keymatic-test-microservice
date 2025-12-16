package com.kymatic.tenantservice.dto.rbac;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AssignRoleRequest(
    @NotNull UUID userId,
    @NotBlank String roleKey,
    UUID siteId,
    OffsetDateTime expiresAt
) {}


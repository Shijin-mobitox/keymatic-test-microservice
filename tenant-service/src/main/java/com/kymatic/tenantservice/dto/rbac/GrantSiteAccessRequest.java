package com.kymatic.tenantservice.dto.rbac;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GrantSiteAccessRequest(
    @NotNull UUID userId,
    @NotNull UUID siteId,
    @NotBlank String accessLevel,
    OffsetDateTime expiresAt
) {}


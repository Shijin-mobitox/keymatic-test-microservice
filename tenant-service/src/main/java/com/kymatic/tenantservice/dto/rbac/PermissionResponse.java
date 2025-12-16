package com.kymatic.tenantservice.dto.rbac;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PermissionResponse(
    UUID permissionId,
    String permissionKey,
    String permissionName,
    String description,
    String category,
    String resource,
    String action,
    OffsetDateTime createdAt
) {}


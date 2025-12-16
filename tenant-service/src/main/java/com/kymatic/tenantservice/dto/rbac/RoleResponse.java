package com.kymatic.tenantservice.dto.rbac;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record RoleResponse(
    UUID roleId,
    String roleName,
    String roleKey,
    String description,
    Integer level,
    Boolean systemRole,
    Boolean active,
    Set<String> permissionKeys,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}


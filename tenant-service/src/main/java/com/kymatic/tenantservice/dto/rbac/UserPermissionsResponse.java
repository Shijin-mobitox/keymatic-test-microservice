package com.kymatic.tenantservice.dto.rbac;

import java.util.List;
import java.util.UUID;

public record UserPermissionsResponse(
    UUID userId,
    UUID siteId,
    List<String> permissions
) {}


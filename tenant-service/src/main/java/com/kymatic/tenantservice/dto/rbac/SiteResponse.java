package com.kymatic.tenantservice.dto.rbac;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SiteResponse(
    UUID siteId,
    String siteName,
    String siteCode,
    String address,
    String city,
    String state,
    String country,
    String postalCode,
    String phone,
    String email,
    Boolean headquarters,
    Boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}


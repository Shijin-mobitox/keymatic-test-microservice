package com.kymatic.tenantservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantStatusUpdateResponse(
	UUID tenantId,
	String status,
	String message,
	OffsetDateTime updatedAt
) { }



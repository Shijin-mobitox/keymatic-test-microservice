package com.kymatic.tenantservice.dto.tenant;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectResponse(
	UUID projectId,
	String name,
	String description,
	UUID ownerId,
	String status,
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt
) {}


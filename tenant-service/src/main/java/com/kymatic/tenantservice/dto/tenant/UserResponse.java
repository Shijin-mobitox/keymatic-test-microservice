package com.kymatic.tenantservice.dto.tenant;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
	UUID userId,
	String email,
	String firstName,
	String lastName,
	String role,
	Boolean isActive,
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt
) {}


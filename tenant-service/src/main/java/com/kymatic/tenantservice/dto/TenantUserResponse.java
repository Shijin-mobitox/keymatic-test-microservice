package com.kymatic.tenantservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantUserResponse(
	UUID userId,
	UUID tenantId,
	String email,
	String role,
	Boolean isActive,
	OffsetDateTime lastLogin,
	OffsetDateTime createdAt
) {}


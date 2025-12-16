package com.kymatic.tenantservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantMigrationResponse(
	Long migrationId,
	UUID tenantId,
	String version,
	String status,
	OffsetDateTime appliedAt
) { }



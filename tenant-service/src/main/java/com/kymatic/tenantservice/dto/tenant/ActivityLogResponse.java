package com.kymatic.tenantservice.dto.tenant;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ActivityLogResponse(
	Long logId,
	UUID userId,
	String action,
	String entityType,
	UUID entityId,
	JsonNode changes,
	OffsetDateTime createdAt
) {}


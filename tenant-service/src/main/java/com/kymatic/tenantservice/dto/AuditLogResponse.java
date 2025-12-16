package com.kymatic.tenantservice.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
	Long logId,
	UUID tenantId,
	String action,
	UUID performedBy,
	JsonNode details,
	OffsetDateTime createdAt
) {}


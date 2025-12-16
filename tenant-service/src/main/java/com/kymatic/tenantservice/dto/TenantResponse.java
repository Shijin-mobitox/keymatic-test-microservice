package com.kymatic.tenantservice.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantResponse(
	UUID tenantId,
	String tenantName,
	String slug,
	String status,
	String subscriptionTier,
	String databaseConnectionString,
	String databaseName,
	Integer maxUsers,
	Integer maxStorageGb,
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt,
	JsonNode metadata
) { }



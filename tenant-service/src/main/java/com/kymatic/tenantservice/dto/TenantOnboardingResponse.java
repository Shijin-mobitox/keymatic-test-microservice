package com.kymatic.tenantservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for tenant onboarding with Keycloak Organizations integration.
 */
public record TenantOnboardingResponse(
	@Schema(description = "Tenant ID (from master database)")
	UUID tenantId,

	@Schema(description = "Tenant name")
	String tenantName,

	@Schema(description = "Tenant slug (used as organization alias)")
	String slug,

	@Schema(description = "Keycloak organization ID")
	String keycloakOrganizationId,

	@Schema(description = "Keycloak organization alias")
	String keycloakOrganizationAlias,

	@Schema(description = "Database name")
	String databaseName,

	@Schema(description = "Database connection string")
	String databaseConnectionString,

	@Schema(description = "Status")
	String status,

	@Schema(description = "Initial admin user ID (from Keycloak)")
	String adminUserId,

	@Schema(description = "Initial admin user email")
	String adminUserEmail,

	@Schema(description = "Created at timestamp")
	OffsetDateTime createdAt,

	@Schema(description = "Metadata")
	JsonNode metadata
) { }


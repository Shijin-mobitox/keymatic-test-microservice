package com.kymatic.tenantservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for user onboarding under a tenant (organization).
 */
public record UserOnboardingResponse(
	@Schema(description = "User ID (from Keycloak)")
	String userId,

	@Schema(description = "User email")
	String email,

	@Schema(description = "User first name")
	String firstName,

	@Schema(description = "User last name")
	String lastName,

	@Schema(description = "Organization ID (from Keycloak)")
	String organizationId,

	@Schema(description = "Organization alias")
	String organizationAlias,

	@Schema(description = "Assigned role")
	String role
) { }


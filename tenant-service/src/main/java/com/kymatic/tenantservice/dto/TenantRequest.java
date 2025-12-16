package com.kymatic.tenantservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TenantRequest(
	@Schema(description = "Display name", example = "Acme Corp")
	@NotBlank
	@Size(max = 255)
	String tenantName,

	@Schema(description = "URL-friendly identifier", example = "acme")
	@NotBlank
	@Pattern(regexp = "^[a-z0-9\\-]+$", message = "Slug must contain lowercase letters, digits or hyphens")
	@Size(max = 100)
	String slug,

	@Schema(description = "Subscription tier", example = "starter")
	@NotBlank
	String subscriptionTier,

	@Schema(description = "Max users", example = "100")
	@NotNull
	Integer maxUsers,

	@Schema(description = "Max storage in GB", example = "50")
	@NotNull
	Integer maxStorageGb,

	@Schema(description = "Admin user email for the tenant", example = "admin@acme.com")
	@Email
	String adminEmail,

	@Schema(description = "Flexible metadata")
	JsonNode metadata
) { }



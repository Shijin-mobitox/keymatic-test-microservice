package com.kymatic.tenantservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new tenant with Keycloak Organizations integration.
 * 
 * This request will:
 * 1. Create an organization in Keycloak
 * 2. Create and initialize tenant database
 * 3. Create initial admin user(s) in Keycloak and assign to organization
 */
public record CreateTenantRequest(
	@Schema(description = "Display name for the tenant/organization", example = "Acme Corp")
	@NotBlank(message = "Tenant name is required")
	@Size(max = 255, message = "Tenant name must not exceed 255 characters")
	String tenantName,

	@Schema(description = "URL-friendly identifier (used as organization alias in Keycloak)", example = "acme")
	@NotBlank(message = "Slug is required")
	@Pattern(regexp = "^[a-z0-9\\-]+$", message = "Slug must contain only lowercase letters, digits, or hyphens")
	@Size(max = 100, message = "Slug must not exceed 100 characters")
	String slug,

	@Schema(description = "Subscription tier", example = "starter")
	@NotBlank(message = "Subscription tier is required")
	String subscriptionTier,

	@Schema(description = "Maximum number of users allowed", example = "100")
	@NotNull(message = "Max users is required")
	Integer maxUsers,

	@Schema(description = "Maximum storage in GB", example = "50")
	@NotNull(message = "Max storage GB is required")
	Integer maxStorageGb,

	@Schema(description = "Initial admin user details (preferred format)")
	@Valid
	AdminUserRequest adminUser,

	@Schema(description = "Admin user email (legacy format - used if adminUser is not provided)", example = "admin@tenant.com")
	@Email(message = "Admin email must be valid")
	String adminEmail,

	@Schema(description = "Flexible metadata as JSON")
	JsonNode metadata
) {
	/**
	 * Nested DTO for admin user creation.
	 */
	public record AdminUserRequest(
		@Schema(description = "Admin user email (used as username in Keycloak)", example = "admin@acme.com")
		@NotBlank(message = "Admin email is required")
		@Email(message = "Admin email must be valid")
		String email,

		@Schema(description = "Admin user password", example = "SecurePassword123!")
		@NotBlank(message = "Admin password is required")
		@Size(min = 8, message = "Password must be at least 8 characters")
		String password,

		@Schema(description = "Admin user first name", example = "John")
		@NotBlank(message = "First name is required")
		@Size(max = 255)
		String firstName,

		@Schema(description = "Admin user last name", example = "Doe")
		@NotBlank(message = "Last name is required")
		@Size(max = 255)
		String lastName,

		@Schema(description = "Whether email is verified", example = "true")
		Boolean emailVerified
	) {
		public Boolean emailVerified() {
			return emailVerified != null ? emailVerified : false;
		}
	}
}


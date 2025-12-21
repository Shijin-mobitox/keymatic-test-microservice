package com.kymatic.tenantservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new user under a tenant (organization).
 * 
 * This will create a user in Keycloak and assign them to the organization.
 */
public record CreateUserRequest(
	@Schema(description = "User email (used as username in Keycloak)", example = "user@acme.com")
	@NotBlank(message = "Email is required")
	@Email(message = "Email must be valid")
	String email,

	@Schema(description = "User password", example = "SecurePassword123!")
	@NotBlank(message = "Password is required")
	@Size(min = 8, message = "Password must be at least 8 characters")
	String password,

	@Schema(description = "User first name", example = "Jane")
	@NotBlank(message = "First name is required")
	@Size(max = 255)
	String firstName,

	@Schema(description = "User last name", example = "Smith")
	@NotBlank(message = "Last name is required")
	@Size(max = 255)
	String lastName,

	@Schema(description = "Whether email is verified", example = "true")
	Boolean emailVerified,

	@Schema(description = "Role to assign to user in the organization", example = "user")
	String role
) {
	public Boolean emailVerified() {
		return emailVerified != null ? emailVerified : false;
	}
	
	public String role() {
		return role != null && !role.isBlank() ? role : "user";
	}
}


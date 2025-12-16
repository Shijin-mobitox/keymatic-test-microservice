package com.kymatic.tenantservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TenantUserRequest(
	@NotBlank(message = "Tenant ID is required")
	String tenantId,
	
	@NotBlank(message = "Email is required")
	@Email(message = "Email must be valid")
	String email,
	
	@NotBlank(message = "Password is required")
	String password,
	
	@NotBlank(message = "Role is required")
	String role, // admin, user, etc.
	
	Boolean isActive
) {
	public Boolean isActive() {
		return isActive != null ? isActive : true;
	}
}


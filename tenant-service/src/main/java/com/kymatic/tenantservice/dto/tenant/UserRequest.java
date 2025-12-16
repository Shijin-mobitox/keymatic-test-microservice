package com.kymatic.tenantservice.dto.tenant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRequest(
	@NotBlank(message = "Email is required")
	@Email(message = "Email must be valid")
	String email,
	
	String firstName,
	
	String lastName,
	
	String role,
	
	Boolean isActive
) {
	public Boolean isActive() {
		return isActive != null ? isActive : true;
	}
}


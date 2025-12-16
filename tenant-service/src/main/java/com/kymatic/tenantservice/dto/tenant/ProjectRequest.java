package com.kymatic.tenantservice.dto.tenant;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ProjectRequest(
	@NotBlank(message = "Name is required")
	String name,
	
	String description,
	
	UUID ownerId,
	
	String status
) {}


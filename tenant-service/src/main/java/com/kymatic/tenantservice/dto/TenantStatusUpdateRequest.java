package com.kymatic.tenantservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TenantStatusUpdateRequest(
	@Schema(description = "New status", example = "suspended")
	@NotBlank
	String status
) { }



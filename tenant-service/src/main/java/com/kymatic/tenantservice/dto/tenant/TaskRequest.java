package com.kymatic.tenantservice.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record TaskRequest(
	@NotNull(message = "Project ID is required")
	UUID projectId,
	
	@NotBlank(message = "Title is required")
	String title,
	
	String description,
	
	UUID assignedTo,
	
	String status,
	
	LocalDate dueDate
) {}


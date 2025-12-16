package com.kymatic.tenantservice.dto.tenant;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TaskResponse(
	UUID taskId,
	UUID projectId,
	String title,
	String description,
	UUID assignedTo,
	String status,
	LocalDate dueDate,
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt
) {}


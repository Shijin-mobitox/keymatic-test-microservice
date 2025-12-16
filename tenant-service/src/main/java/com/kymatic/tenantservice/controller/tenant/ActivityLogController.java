package com.kymatic.tenantservice.controller.tenant;

import com.kymatic.tenantservice.dto.tenant.ActivityLogResponse;
import com.kymatic.tenantservice.service.tenant.ActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/activity-logs", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Activity Log", description = "View tenant-specific activity logs")
public class ActivityLogController {

	private final ActivityLogService activityLogService;

	public ActivityLogController(ActivityLogService activityLogService) {
		this.activityLogService = activityLogService;
	}

	@Operation(summary = "Get all activity logs")
	@GetMapping
	public ResponseEntity<List<ActivityLogResponse>> getAllActivityLogs(
		@Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
		@Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
	) {
		if (size > 0) {
			Pageable pageable = PageRequest.of(page, size);
			Page<ActivityLogResponse> response = activityLogService.getActivityLogs(pageable);
			return ResponseEntity.ok(response.getContent());
		} else {
			List<ActivityLogResponse> response = activityLogService.getAllActivityLogs();
			return ResponseEntity.ok(response);
		}
	}

	@Operation(summary = "Get activity log by ID")
	@GetMapping("/{logId}")
	public ResponseEntity<ActivityLogResponse> getActivityLog(
		@Parameter(description = "Log ID") @PathVariable Long logId
	) {
		ActivityLogResponse response = activityLogService.getActivityLog(logId);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Get activity logs by user")
	@GetMapping("/user/{userId}")
	public ResponseEntity<List<ActivityLogResponse>> getActivityLogsByUser(
		@Parameter(description = "User ID") @PathVariable UUID userId
	) {
		List<ActivityLogResponse> response = activityLogService.getActivityLogsByUser(userId);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Get activity logs by action")
	@GetMapping("/action/{action}")
	public ResponseEntity<List<ActivityLogResponse>> getActivityLogsByAction(
		@Parameter(description = "Action type") @PathVariable String action
	) {
		List<ActivityLogResponse> response = activityLogService.getActivityLogsByAction(action);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Get activity logs by entity")
	@GetMapping("/entity/{entityType}/{entityId}")
	public ResponseEntity<List<ActivityLogResponse>> getActivityLogsByEntity(
		@Parameter(description = "Entity type") @PathVariable String entityType,
		@Parameter(description = "Entity ID") @PathVariable UUID entityId
	) {
		List<ActivityLogResponse> response = activityLogService.getActivityLogsByEntity(entityType, entityId);
		return ResponseEntity.ok(response);
	}
}


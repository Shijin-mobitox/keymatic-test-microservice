package com.kymatic.tenantservice.controller;

import com.kymatic.tenantservice.dto.AuditLogResponse;
import com.kymatic.tenantservice.service.AuditLogService;
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
@RequestMapping(path = "/api/audit-logs", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Audit Log", description = "View tenant audit logs")
public class AuditLogController {

	private final AuditLogService auditLogService;

	public AuditLogController(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@Operation(summary = "Get audit log by ID")
	@GetMapping("/{logId}")
	public ResponseEntity<AuditLogResponse> getAuditLog(
		@Parameter(description = "Log ID") @PathVariable Long logId
	) {
		AuditLogResponse response = auditLogService.getAuditLog(logId);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Get all audit logs for a tenant")
	@GetMapping("/tenant/{tenantId}")
	public ResponseEntity<List<AuditLogResponse>> getAuditLogsByTenant(
		@Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
		@Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
		@Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size
	) {
		if (size > 0) {
			Pageable pageable = PageRequest.of(page, size);
			Page<AuditLogResponse> response = auditLogService.getAuditLogsByTenant(tenantId, pageable);
			return ResponseEntity.ok(response.getContent());
		} else {
			List<AuditLogResponse> response = auditLogService.getAuditLogsByTenant(tenantId);
			return ResponseEntity.ok(response);
		}
	}

	@Operation(summary = "Get audit logs by action")
	@GetMapping("/tenant/{tenantId}/action/{action}")
	public ResponseEntity<List<AuditLogResponse>> getAuditLogsByAction(
		@Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
		@Parameter(description = "Action type") @PathVariable String action
	) {
		List<AuditLogResponse> response = auditLogService.getAuditLogsByAction(tenantId, action);
		return ResponseEntity.ok(response);
	}
}


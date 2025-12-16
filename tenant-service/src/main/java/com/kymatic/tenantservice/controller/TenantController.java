package com.kymatic.tenantservice.controller;

import com.kymatic.tenantservice.dto.TenantMigrationResponse;
import com.kymatic.tenantservice.dto.TenantRequest;
import com.kymatic.tenantservice.dto.TenantResponse;
import com.kymatic.tenantservice.dto.TenantStatusUpdateRequest;
import com.kymatic.tenantservice.dto.TenantStatusUpdateResponse;
import com.kymatic.tenantservice.dto.workflow.WorkflowProcessResponse;
import com.kymatic.tenantservice.persistence.entity.TenantEntity;
import com.kymatic.tenantservice.persistence.entity.TenantMigrationEntity;
import com.kymatic.tenantservice.service.TenantProvisioningService;
import com.kymatic.tenantservice.service.WorkflowOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/api/tenants", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tenant Management", description = "Tenant provisioning and routing (master database).")
public class TenantController {

	private final TenantProvisioningService tenantProvisioningService;
	private final WorkflowOrchestrationService workflowOrchestrationService;

	public TenantController(
		TenantProvisioningService tenantProvisioningService,
		WorkflowOrchestrationService workflowOrchestrationService
	) {
		this.tenantProvisioningService = tenantProvisioningService;
		this.workflowOrchestrationService = workflowOrchestrationService;
	}

	@Operation(
		summary = "Provision a new tenant",
		description = "Creates a new tenant record in the master database and initializes a per-tenant database.",
		responses = {
			@ApiResponse(responseCode = "201", description = "Tenant created",
				content = @Content(schema = @Schema(implementation = TenantResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid input")
		}
	)
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TenantResponse> createTenant(
		@Valid @RequestBody TenantRequest request
	) {
		TenantEntity tenant = tenantProvisioningService.createTenant(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tenant));
	}

	@Operation(
		summary = "Provision a new tenant via workflow",
		description = "Creates a new tenant using Camunda workflow orchestration. Returns process instance ID.",
		responses = {
			@ApiResponse(responseCode = "202", description = "Tenant provisioning workflow started"),
			@ApiResponse(responseCode = "400", description = "Invalid input")
		}
	)
	@PostMapping(value = "/workflow", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> createTenantViaWorkflow(
		@Valid @RequestBody TenantRequest request
	) {
		WorkflowProcessResponse response = workflowOrchestrationService.startTenantProvisioning(request);
		return ResponseEntity.status(HttpStatus.ACCEPTED)
			.body(Map.of(
				"processInstanceId", response.processInstanceId(),
				"status", response.status()
			));
	}

	@Operation(
		summary = "List tenants",
		description = "Returns a list of tenants from the master database."
	)
	@GetMapping
	public ResponseEntity<List<TenantResponse>> listTenants(
		@Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
		@Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size
	) {
		List<TenantResponse> tenants = tenantProvisioningService.listTenants().stream()
			.map(this::toResponse)
			.toList();
		return ResponseEntity.ok(tenants);
	}

	@Operation(
		summary = "Get tenant by ID",
		responses = {
			@ApiResponse(responseCode = "200", description = "Tenant found"),
			@ApiResponse(responseCode = "404", description = "Tenant not found")
		}
	)
	@GetMapping("/{tenantId}")
	public ResponseEntity<TenantResponse> getTenantById(
		@PathVariable UUID tenantId
	) {
		return tenantProvisioningService.getTenant(tenantId)
			.map(this::toResponse)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@Operation(
		summary = "Get tenant by slug",
		responses = {
			@ApiResponse(responseCode = "200", description = "Tenant found"),
			@ApiResponse(responseCode = "404", description = "Tenant not found")
		}
	)
	@GetMapping("/slug/{slug}")
	public ResponseEntity<TenantResponse> getTenantBySlug(@PathVariable String slug) {
		return tenantProvisioningService.getTenantBySlug(slug)
			.map(this::toResponse)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@Operation(
		summary = "Update tenant status",
		description = "Set tenant status to active, suspended, or deleted.",
		responses = @ApiResponse(responseCode = "200", description = "Status updated")
	)
	@PostMapping("/{tenantId}/status")
	public ResponseEntity<TenantStatusUpdateResponse> updateTenantStatus(
		@PathVariable UUID tenantId,
		@Valid @RequestBody TenantStatusUpdateRequest request
	) {
		TenantEntity tenant = tenantProvisioningService.updateStatus(tenantId, request.status());
		TenantStatusUpdateResponse response = new TenantStatusUpdateResponse(
			tenant.getTenantId(),
			tenant.getStatus(),
			"Status updated successfully",
			tenant.getUpdatedAt()
		);
		return ResponseEntity.ok(response);
	}

	@Operation(
		summary = "Update tenant status via workflow",
		description = "Set tenant status using Camunda workflow orchestration. Returns process instance ID.",
		responses = @ApiResponse(responseCode = "202", description = "Status update workflow started")
	)
	@PostMapping("/{tenantId}/status/workflow")
	public ResponseEntity<Map<String, String>> updateTenantStatusViaWorkflow(
		@PathVariable UUID tenantId,
		@Valid @RequestBody TenantStatusUpdateRequest request
	) {
		WorkflowProcessResponse response = workflowOrchestrationService.startTenantStatusUpdate(tenantId, request);
		return ResponseEntity.status(HttpStatus.ACCEPTED)
			.body(Map.of(
				"processInstanceId", response.processInstanceId(),
				"status", response.status()
			));
	}

	@Operation(
		summary = "Run tenant schema migrations",
		description = "Trigger migrations for a tenant database and record status in master DB.",
		responses = @ApiResponse(responseCode = "202", description = "Migration started")
	)
	@PostMapping("/{tenantId}/migrations/run")
	public ResponseEntity<List<TenantMigrationResponse>> runMigrations(@PathVariable UUID tenantId) {
		List<String> applied = tenantProvisioningService.runMigrations(tenantId);
		Set<String> appliedSet = applied.stream()
			.filter(version -> version != null && !version.isBlank())
			.collect(Collectors.toSet());

		List<TenantMigrationResponse> responses = tenantProvisioningService.listMigrations(tenantId).stream()
			.filter(migration -> appliedSet.contains(migration.getVersion()))
			.map(this::toMigrationResponse)
			.toList();

		return ResponseEntity.status(HttpStatus.ACCEPTED).body(responses);
	}

	@Operation(
		summary = "List tenant migrations",
		description = "Returns migration history for a tenant."
	)
	@GetMapping("/{tenantId}/migrations")
	public ResponseEntity<List<TenantMigrationResponse>> listMigrations(@PathVariable UUID tenantId) {
		List<TenantMigrationResponse> list = tenantProvisioningService.listMigrations(tenantId).stream()
			.map(this::toMigrationResponse)
			.toList();
		return ResponseEntity.ok(list);
	}

	private TenantResponse toResponse(TenantEntity entity) {
		return new TenantResponse(
			entity.getTenantId(),
			entity.getTenantName(),
			entity.getSlug(),
			entity.getStatus(),
			entity.getSubscriptionTier(),
			entity.getDatabaseConnectionString(),
			entity.getDatabaseName(),
			entity.getMaxUsers(),
			entity.getMaxStorageGb(),
			entity.getCreatedAt(),
			entity.getUpdatedAt(),
			entity.getMetadata()
		);
	}

	private TenantMigrationResponse toMigrationResponse(TenantMigrationEntity entity) {
		return new TenantMigrationResponse(
			entity.getMigrationId(),
			entity.getTenantId(),
			entity.getVersion(),
			entity.getStatus(),
			entity.getAppliedAt()
		);
	}
}

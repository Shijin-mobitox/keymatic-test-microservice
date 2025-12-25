package com.kymatic.tenantservice.controller;

import com.kymatic.tenantservice.dto.CreateTenantRequest;
import com.kymatic.tenantservice.dto.CreateUserRequest;
import com.kymatic.tenantservice.dto.TenantMigrationResponse;
import com.kymatic.tenantservice.dto.TenantOnboardingResponse;
import com.kymatic.tenantservice.dto.TenantRequest;
import com.kymatic.tenantservice.dto.TenantResponse;
import com.kymatic.tenantservice.dto.TenantStatusUpdateRequest;
import com.kymatic.tenantservice.dto.TenantStatusUpdateResponse;
import com.kymatic.tenantservice.dto.UserOnboardingResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger logger = LoggerFactory.getLogger(TenantController.class);

	private final TenantProvisioningService tenantProvisioningService;
	private final WorkflowOrchestrationService workflowOrchestrationService;
	private final com.kymatic.tenantservice.service.TenantOnboardingService tenantOnboardingService;
	private final com.kymatic.tenantservice.client.KeycloakClientWrapper keycloakClientWrapper;

	public TenantController(
		TenantProvisioningService tenantProvisioningService,
		WorkflowOrchestrationService workflowOrchestrationService,
		com.kymatic.tenantservice.service.TenantOnboardingService tenantOnboardingService,
		com.kymatic.tenantservice.client.KeycloakClientWrapper keycloakClientWrapper
	) {
		this.tenantProvisioningService = tenantProvisioningService;
		this.workflowOrchestrationService = workflowOrchestrationService;
		this.tenantOnboardingService = tenantOnboardingService;
		this.keycloakClientWrapper = keycloakClientWrapper;
	}

	@Operation(
		summary = "Provision a new tenant (legacy - without Keycloak Organizations)",
		description = "Creates a new tenant record in the master database and initializes a per-tenant database. " +
			"This endpoint does NOT create a Keycloak organization. Use POST /api/tenants/onboard for full Keycloak integration.",
		responses = {
			@ApiResponse(responseCode = "201", description = "Tenant created",
				content = @Content(schema = @Schema(implementation = TenantResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid input")
		}
	)
	@PostMapping(value = "/legacy", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TenantResponse> createTenantLegacy(
		@Valid @RequestBody TenantRequest request
	) {
		TenantEntity tenant = tenantProvisioningService.createTenant(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tenant));
	}

	@Operation(
		summary = "Onboard a new tenant with Keycloak Organizations",
		description = "Creates a new tenant with full Keycloak Organizations integration: " +
			"1. Creates organization in Keycloak, " +
			"2. Creates and initializes tenant database, " +
			"3. Creates initial admin user in Keycloak and assigns to organization. " +
			"This is the recommended endpoint for new tenant creation.",
		responses = {
			@ApiResponse(responseCode = "201", description = "Tenant onboarded successfully",
				content = @Content(schema = @Schema(implementation = TenantOnboardingResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid input"),
			@ApiResponse(responseCode = "409", description = "Tenant or organization already exists")
		}
	)
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TenantOnboardingResponse> createTenant(
		@Valid @RequestBody CreateTenantRequest request
	) {
		TenantOnboardingResponse response = tenantOnboardingService.createTenant(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(
		summary = "Create a new user under a tenant (organization)",
		description = "Creates a new user in Keycloak and assigns them to the tenant's organization. " +
			"The tenant is identified by its slug (organization alias).",
		responses = {
			@ApiResponse(responseCode = "201", description = "User created successfully",
				content = @Content(schema = @Schema(implementation = UserOnboardingResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid input"),
			@ApiResponse(responseCode = "404", description = "Tenant not found"),
			@ApiResponse(responseCode = "409", description = "User already exists")
		}
	)
	@PostMapping(value = "/{tenantAlias}/users", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<UserOnboardingResponse> createUserForTenant(
		@Parameter(description = "Tenant alias (slug)") @PathVariable String tenantAlias,
		@Valid @RequestBody CreateUserRequest request
	) {
		UserOnboardingResponse response = tenantOnboardingService.createUserForTenant(tenantAlias, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

	@Operation(
		summary = "Manually assign user to organization", 
		description = "Assigns a user to an organization in Keycloak using the SAFE assignment flow with verification. " +
					  "This endpoint handles Keycloak 26.x timing issues and provides reliable user-organization assignment.",
		responses = {
			@ApiResponse(responseCode = "200", description = "User assigned successfully (verified)"),
			@ApiResponse(responseCode = "400", description = "Assignment failed or user/organization not found"),
			@ApiResponse(responseCode = "409", description = "User is already assigned to organization")
		}
	)
	@PostMapping("/{slug}/assign-user")
	public ResponseEntity<Map<String, Object>> assignUserToOrganization(
		@Parameter(description = "Organization alias (slug)", example = "orgtenant12") @PathVariable String slug,
		@Parameter(description = "User email to assign", example = "user@example.com") @RequestParam String userEmail
	) {
		logger.info("🔧 API REQUEST: Manual user assignment - slug={}, email={}", slug, userEmail);
		
		// Input validation
		if (slug == null || slug.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of(
				"success", false,
				"message", "Organization slug is required",
				"error", "INVALID_INPUT"
			));
		}
		
		if (userEmail == null || userEmail.isBlank() || !userEmail.contains("@")) {
			return ResponseEntity.badRequest().body(Map.of(
				"success", false,
				"message", "Valid user email is required",
				"error", "INVALID_EMAIL"
			));
		}
		
		try {
			// Use the same verification logic as automatic assignment
			boolean success = keycloakClientWrapper.manuallyAssignUserToOrganization(slug, userEmail);
			
			if (success) {
				logger.info("✅ API SUCCESS: User {} assigned to organization {} via API", userEmail, slug);
				return ResponseEntity.ok(Map.of(
					"success", true,
					"message", "User successfully assigned to organization and verified",
					"organizationSlug", slug,
					"userEmail", userEmail,
					"status", "VERIFIED_ASSIGNED",
					"timestamp", System.currentTimeMillis()
				));
			} else {
				logger.warn("❌ API FAILURE: Could not assign user {} to organization {}", userEmail, slug);
				return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", "Failed to assign user to organization despite multiple attempts",
					"organizationSlug", slug,
					"userEmail", userEmail,
					"status", "ASSIGNMENT_FAILED",
					"remediation", Map.of(
						"gui_method", "http://localhost:8085 → kymatic realm → Organizations → " + slug + " → Members → Add member",
						"search_email", userEmail,
						"reason", "Keycloak 26.x Organizations API timing limitations"
					),
					"timestamp", System.currentTimeMillis()
				));
			}
		} catch (Exception e) {
			logger.error("❌ API ERROR: Exception during manual assignment - slug={}, email={}, error={}", 
				slug, userEmail, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
				"success", false,
				"message", "Internal error during user assignment",
				"organizationSlug", slug,
				"userEmail", userEmail,
				"error", e.getMessage(),
				"status", "INTERNAL_ERROR",
				"timestamp", System.currentTimeMillis()
			));
		}
	}
}

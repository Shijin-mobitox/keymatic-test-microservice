package com.kymatic.tenantservice.controller;

import com.kymatic.tenantservice.dto.TenantUserRequest;
import com.kymatic.tenantservice.dto.TenantUserResponse;
import com.kymatic.tenantservice.service.TenantUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/tenant-users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tenant User Management", description = "Manage users within tenants (master database)")
public class TenantUserController {

	private final TenantUserService tenantUserService;

	public TenantUserController(TenantUserService tenantUserService) {
		this.tenantUserService = tenantUserService;
	}

	@Operation(summary = "Create a new tenant user", description = "Creates a user for a tenant in the master database")
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TenantUserResponse> createTenantUser(
		@Valid @RequestBody TenantUserRequest request
	) {
		TenantUserResponse response = tenantUserService.createTenantUser(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Get tenant user by ID")
	@GetMapping("/{userId}")
	public ResponseEntity<TenantUserResponse> getTenantUser(
		@Parameter(description = "User ID") @PathVariable UUID userId
	) {
		TenantUserResponse response = tenantUserService.getTenantUser(userId);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Get tenant user by email")
	@GetMapping("/tenant/{tenantId}/email")
	public ResponseEntity<TenantUserResponse> getTenantUserByEmail(
		@Parameter(description = "Tenant ID or slug") @PathVariable String tenantId,
		@Parameter(description = "Email address") @RequestParam String email
	) {
		TenantUserResponse response = tenantUserService.getTenantUserByEmail(tenantId, email);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Get all users for a tenant")
	@GetMapping("/tenant/{tenantId}")
	public ResponseEntity<List<TenantUserResponse>> getUsersByTenant(
		@Parameter(description = "Tenant ID or slug") @PathVariable String tenantId
	) {
		List<TenantUserResponse> users = tenantUserService.getUsersByTenant(tenantId);
		return ResponseEntity.ok(users);
	}

	@Operation(summary = "Update tenant user")
	@PutMapping("/{userId}")
	public ResponseEntity<TenantUserResponse> updateTenantUser(
		@Parameter(description = "User ID") @PathVariable UUID userId,
		@Valid @RequestBody TenantUserRequest request
	) {
		TenantUserResponse response = tenantUserService.updateTenantUser(userId, request);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Delete tenant user")
	@DeleteMapping("/{userId}")
	@ApiResponse(responseCode = "204", description = "User deleted")
	public ResponseEntity<Void> deleteTenantUser(
		@Parameter(description = "User ID") @PathVariable UUID userId
	) {
		tenantUserService.deleteTenantUser(userId);
		return ResponseEntity.noContent().build();
	}
}


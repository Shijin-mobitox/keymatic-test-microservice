package com.kymatic.tenantservice.controller;

import com.kymatic.tenantservice.dto.SubscriptionRequest;
import com.kymatic.tenantservice.dto.SubscriptionResponse;
import com.kymatic.tenantservice.service.SubscriptionService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Subscription Management", description = "Manage tenant subscriptions and billing")
public class SubscriptionController {

	private final SubscriptionService subscriptionService;

	public SubscriptionController(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}

	@Operation(summary = "Create a new subscription", description = "Creates a subscription for a tenant")
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<SubscriptionResponse> createSubscription(
		@Valid @RequestBody SubscriptionRequest request
	) {
		SubscriptionResponse response = subscriptionService.createSubscription(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Get subscription by ID")
	@GetMapping("/{subscriptionId}")
	public ResponseEntity<SubscriptionResponse> getSubscription(
		@Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId
	) {
		SubscriptionResponse response = subscriptionService.getSubscription(subscriptionId);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Get all subscriptions for a tenant")
	@GetMapping("/tenant/{tenantId}")
	public ResponseEntity<List<SubscriptionResponse>> getSubscriptionsByTenant(
		@Parameter(description = "Tenant ID or slug") @PathVariable String tenantId
	) {
		List<SubscriptionResponse> subscriptions = subscriptionService.getSubscriptionsByTenant(tenantId);
		return ResponseEntity.ok(subscriptions);
	}

	@Operation(summary = "Update subscription")
	@PutMapping("/{subscriptionId}")
	public ResponseEntity<SubscriptionResponse> updateSubscription(
		@Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
		@Valid @RequestBody SubscriptionRequest request
	) {
		SubscriptionResponse response = subscriptionService.updateSubscription(subscriptionId, request);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Delete subscription")
	@DeleteMapping("/{subscriptionId}")
	@ApiResponse(responseCode = "204", description = "Subscription deleted")
	public ResponseEntity<Void> deleteSubscription(
		@Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId
	) {
		subscriptionService.deleteSubscription(subscriptionId);
		return ResponseEntity.noContent().build();
	}
}


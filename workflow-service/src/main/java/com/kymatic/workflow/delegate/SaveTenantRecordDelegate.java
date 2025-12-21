package com.kymatic.workflow.delegate;

import com.kymatic.workflow.service.TenantServiceClient;
import com.kymatic.workflow.dto.TenantRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Camunda delegate for saving tenant record in tenant-service master database.
 * 
 * Expected workflow variables:
 * - tenantName: Tenant name
 * - slug: Tenant slug
 * - subscriptionTier: Subscription tier
 * - maxUsers: Max users
 * - maxStorageGb: Max storage GB
 * - databaseName: Database name
 * - databaseConnectionString: Database connection string
 * - keycloakOrganizationId: Keycloak organization ID
 * - adminUserId: Admin user ID
 * - adminEmail: Admin email
 * - metadata: Optional metadata
 * 
 * Sets workflow variables:
 * - tenantId: Saved tenant ID
 * - tenantRecordSaved: true
 */
@Component("saveTenantRecordDelegate")
public class SaveTenantRecordDelegate implements JavaDelegate {

	private static final Logger logger = LoggerFactory.getLogger(SaveTenantRecordDelegate.class);

	private final TenantServiceClient tenantServiceClient;
	private final ObjectMapper objectMapper;

	public SaveTenantRecordDelegate(TenantServiceClient tenantServiceClient, ObjectMapper objectMapper) {
		this.tenantServiceClient = tenantServiceClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		String tenantName = (String) execution.getVariable("tenantName");
		String slug = (String) execution.getVariable("slug");
		String subscriptionTier = (String) execution.getVariable("subscriptionTier");
		Integer maxUsers = (Integer) execution.getVariable("maxUsers");
		Integer maxStorageGb = (Integer) execution.getVariable("maxStorageGb");
		String databaseName = (String) execution.getVariable("databaseName");
		String databaseConnectionString = (String) execution.getVariable("databaseConnectionString");
		String adminEmail = (String) execution.getVariable("adminEmail");
		String adminPassword = (String) execution.getVariable("adminPassword");
		String adminFirstName = (String) execution.getVariable("adminFirstName");
		String adminLastName = (String) execution.getVariable("adminLastName");
		Boolean adminEmailVerified = (Boolean) execution.getVariable("adminEmailVerified");
		Object metadata = execution.getVariable("metadata");

		// Build tenant request - note: we're using the legacy endpoint that doesn't require Keycloak
		// The tenant-service will save the record with the database info
		TenantRequest request = new TenantRequest(
			tenantName,
			slug,
			subscriptionTier,
			maxUsers,
			maxStorageGb,
			adminEmail != null ? adminEmail : "admin@" + slug + ".com", // adminEmail - default if not provided
			adminPassword != null ? adminPassword : "TempPassword123!", // adminPassword - default if not provided
			adminFirstName != null ? adminFirstName : "Admin", // adminFirstName - default if not provided
			adminLastName != null ? adminLastName : "User", // adminLastName - default if not provided
			adminEmailVerified != null ? adminEmailVerified : false, // adminEmailVerified
			metadata == null ? null : objectMapper.valueToTree(metadata)
		);

		logger.info("Saving tenant record in tenant-service: slug={}", slug);

		// Call tenant-service to save the record
		// Note: This uses the legacy endpoint which creates DB, but we've already created it
		// We might need a new endpoint that just saves the record without creating DB
		// For now, we'll call it and it should handle the case where DB already exists
		var response = tenantServiceClient.createTenant(request);

		execution.setVariable("tenantId", response.tenantId().toString());
		execution.setVariable("tenantRecordSaved", true);

		logger.info("Tenant record saved successfully: tenantId={}, slug={}", response.tenantId(), slug);
	}
}


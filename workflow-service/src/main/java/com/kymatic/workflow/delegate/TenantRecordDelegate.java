package com.kymatic.workflow.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kymatic.workflow.persistence.entity.TenantEntity;
import com.kymatic.workflow.persistence.repository.TenantRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Camunda delegate for saving tenant record directly to the workflow-service database.
 * This eliminates the circular dependency with tenant-service.
 * 
 * Expected workflow variables:
 * - tenantName: Tenant name
 * - slug: Tenant slug
 * - subscriptionTier: Subscription tier
 * - maxUsers: Max users
 * - maxStorageGb: Max storage GB
 * - databaseName: Database name (from DatabaseCreationDelegate)
 * - databaseConnectionString: Database connection string (from DatabaseCreationDelegate)
 * - metadata: Optional metadata
 * 
 * Sets workflow variables:
 * - tenantId: Saved tenant ID
 * - tenantRecordSaved: true
 */
@Component("tenantRecordDelegate")
public class TenantRecordDelegate implements JavaDelegate {

	private static final Logger logger = LoggerFactory.getLogger(TenantRecordDelegate.class);
	private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9\\-]+$");

	private final TenantRepository tenantRepository;
	private final ObjectMapper objectMapper;

	public TenantRecordDelegate(TenantRepository tenantRepository, ObjectMapper objectMapper) {
		this.tenantRepository = tenantRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
	public void execute(DelegateExecution execution) throws Exception {
		String tenantName = (String) execution.getVariable("tenantName");
		String slug = (String) execution.getVariable("slug");
		String subscriptionTier = (String) execution.getVariable("subscriptionTier");
		Integer maxUsers = (Integer) execution.getVariable("maxUsers");
		Integer maxStorageGb = (Integer) execution.getVariable("maxStorageGb");
		String databaseName = (String) execution.getVariable("databaseName");
		String databaseConnectionString = (String) execution.getVariable("databaseConnectionString");
		Object metadata = execution.getVariable("metadata");

		// Validate required fields
		if (tenantName == null || tenantName.isBlank()) {
			throw new IllegalArgumentException("Tenant name is required");
		}
		if (slug == null || slug.isBlank()) {
			throw new IllegalArgumentException("Slug is required");
		}
		if (databaseName == null || databaseName.isBlank()) {
			throw new IllegalArgumentException("Database name is required");
		}
		if (databaseConnectionString == null || databaseConnectionString.isBlank()) {
			throw new IllegalArgumentException("Database connection string is required");
		}

		// Validate slug format
		if (!SLUG_PATTERN.matcher(slug).matches()) {
			throw new IllegalArgumentException("Invalid slug format. Use lowercase letters, digits or hyphens.");
		}

		// Check if slug already exists
		if (tenantRepository.existsBySlug(slug)) {
			throw new IllegalArgumentException("Slug already in use: " + slug);
		}

		// Convert metadata to JsonNode
		JsonNode metadataNode = null;
		if (metadata != null) {
			metadataNode = objectMapper.valueToTree(metadata);
		}

		// Create tenant entity
		TenantEntity tenant = new TenantEntity();
		tenant.setTenantName(tenantName);
		tenant.setSlug(slug);
		tenant.setSubscriptionTier(subscriptionTier);
		tenant.setDatabaseConnectionString(databaseConnectionString);
		tenant.setDatabaseName(databaseName);
		tenant.setMaxUsers(maxUsers);
		tenant.setMaxStorageGb(maxStorageGb);
		tenant.setMetadata(metadataNode);
		tenant.setStatus("active");

		logger.info("Saving tenant record: slug={}, databaseName={}", slug, databaseName);

		// Save tenant record
		TenantEntity saved = tenantRepository.save(tenant);

		// Set workflow variables
		execution.setVariable("tenantId", saved.getTenantId().toString());
		execution.setVariable("tenantRecordSaved", true);

		logger.info("Tenant record saved successfully: tenantId={}, slug={}", saved.getTenantId(), slug);
	}
}

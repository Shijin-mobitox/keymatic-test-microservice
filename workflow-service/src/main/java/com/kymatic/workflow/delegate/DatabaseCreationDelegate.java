package com.kymatic.workflow.delegate;

import com.kymatic.workflow.service.DatabaseCreationService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Camunda delegate for creating and migrating tenant database.
 * 
 * Expected workflow variables:
 * - slug: Tenant slug (used to build database name)
 * 
 * Sets workflow variables:
 * - databaseName: Created database name
 * - databaseConnectionString: JDBC connection string
 * - migrationsApplied: List of applied migration versions
 */
@Component("databaseCreationDelegate")
public class DatabaseCreationDelegate implements JavaDelegate {

	private static final Logger logger = LoggerFactory.getLogger(DatabaseCreationDelegate.class);
	private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9\\-]+$");

	private final DatabaseCreationService databaseCreationService;

	public DatabaseCreationDelegate(DatabaseCreationService databaseCreationService) {
		this.databaseCreationService = databaseCreationService;
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		String slug = (String) execution.getVariable("slug");

		if (slug == null || slug.isBlank()) {
			throw new IllegalArgumentException("Slug is required for database creation");
		}

		// Validate slug
		if (!SLUG_PATTERN.matcher(slug).matches()) {
			throw new IllegalArgumentException("Invalid slug format. Use lowercase letters, digits or hyphens.");
		}

		// Build database name from slug
		String databaseName = buildDatabaseNameFromSlug(slug);
		String databaseConnectionString = databaseCreationService.buildDatabaseConnectionString(databaseName);

		logger.info("Creating tenant database: slug={}, databaseName={}", slug, databaseName);

		// Create database
		databaseCreationService.createDatabaseIfNotExists(databaseName);

		// Run migrations
		List<String> appliedVersions = databaseCreationService.migrateTenantDatabase(databaseName);

		execution.setVariable("databaseName", databaseName);
		execution.setVariable("databaseConnectionString", databaseConnectionString);
		execution.setVariable("migrationsApplied", appliedVersions);
		execution.setVariable("databaseCreated", true);

		logger.info("Database created and migrated successfully: databaseName={}, migrations={}", 
			databaseName, appliedVersions);
	}

	private String buildDatabaseNameFromSlug(String slug) {
		String databaseName = slug.toLowerCase().replace('-', '_');
		
		if (databaseName.isEmpty()) {
			throw new IllegalArgumentException("Invalid slug: results in empty database name");
		}
		
		if (Character.isDigit(databaseName.charAt(0))) {
			databaseName = "t_" + databaseName;
		}
		
		if (databaseName.length() > 63) {
			databaseName = databaseName.substring(0, 63);
		}
		
		return databaseName;
	}

}


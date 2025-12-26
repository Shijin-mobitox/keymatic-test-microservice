package com.kymatic.workflow.service;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Database management service for tenant databases in workflow-service.
 * Handles creation and migration of per-tenant databases.
 */
@Component
public class TenantDatabaseManager {

	private static final Logger logger = LoggerFactory.getLogger(TenantDatabaseManager.class);

	private final String datasourceUrl;
	private final String username;
	private final String password;

	public TenantDatabaseManager(
		@Value("${spring.datasource.url}") String datasourceUrl,
		@Value("${spring.datasource.username}") String username,
		@Value("${spring.datasource.password}") String password
	) {
		this.datasourceUrl = datasourceUrl;
		this.username = username;
		this.password = password;
	}

	public void createDatabaseIfNotExists(String databaseName) {
		if (databaseExists(databaseName)) {
			logger.info("Tenant database '{}' already exists. Skipping creation.", databaseName);
			return;
		}

		logger.info("Creating tenant database: {}", databaseName);
		try (Connection connection = getConnection();
			 Statement statement = connection.createStatement()) {
			statement.execute("CREATE DATABASE \"" + databaseName + "\"");
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to create tenant database " + databaseName, ex);
		}
	}

	public List<String> migrateTenantDatabase(String databaseName) {
		String tenantJdbcUrl = buildTenantJdbcUrl(databaseName);
		logger.info("Running tenant migrations for database {}", tenantJdbcUrl);

		try {
			Flyway flyway = Flyway.configure()
				.dataSource(tenantJdbcUrl, username, password)
				.locations("classpath:db/tenant")
				.baselineOnMigrate(true)
				.validateOnMigrate(true)
				.load();

			// Log discovered migrations
			MigrationInfo[] discoveredMigrations = flyway.info().all();
			logger.info("Discovered {} migration(s) for tenant database {}", discoveredMigrations.length, databaseName);
			for (MigrationInfo migration : discoveredMigrations) {
				logger.info("  - Migration: {} ({}): {}", 
					migration.getVersion() != null ? migration.getVersion().toString() : "baseline", 
					migration.getType(), 
					migration.getDescription());
			}

			MigrateResult result = flyway.migrate();

			if (result.migrations == null || result.migrations.isEmpty()) {
				logger.warn("No migrations were applied to tenant database {}. This might indicate the database is already up to date or migrations were not found.", databaseName);
			} else {
				logger.info("Successfully applied {} migration(s) to tenant database {}", result.migrations.size(), databaseName);
				for (var migration : result.migrations) {
					logger.info("  - Applied: {} ({}): {}", 
						migration.version == null ? "baseline" : migration.version,
						migration.type,
						migration.description);
				}
			}

			// Verify migration was successful
			if (result.success) {
				logger.info("Migration completed successfully for tenant database {}", databaseName);
			} else {
				logger.error("Migration completed with warnings for tenant database {}", databaseName);
			}

			return result.migrations.stream()
				.map(migration -> migration.version == null ? "baseline" : migration.version)
				.collect(Collectors.toList());
		} catch (Exception ex) {
			logger.error("Failed to migrate tenant database {}: {}", databaseName, ex.getMessage(), ex);
			throw new IllegalStateException("Failed to migrate tenant database " + databaseName + ": " + ex.getMessage(), ex);
		}
	}

	public String buildDatabaseConnectionString(String databaseName) {
		return buildTenantJdbcUrl(databaseName);
	}

	private boolean databaseExists(String databaseName) {
		String postgresUrl = buildPostgresConnectionUrl();
		try (Connection connection = java.sql.DriverManager.getConnection(postgresUrl, username, password);
			 var statement = connection.prepareStatement("SELECT EXISTS (SELECT 1 FROM pg_database WHERE datname = ?)")) {
			statement.setString(1, databaseName);
			try (var rs = statement.executeQuery()) {
				if (rs.next()) {
					return rs.getBoolean(1);
				}
				return false;
			}
		} catch (Exception ex) {
			logger.error("Failed to check if database exists: {}", databaseName, ex);
			return false;
		}
	}

	private Connection getConnection() throws Exception {
		return java.sql.DriverManager.getConnection(buildPostgresConnectionUrl(), username, password);
	}

	private String buildPostgresConnectionUrl() {
		// Parse the original URL and replace database name with 'postgres' for admin operations
		String baseUrl = datasourceUrl.substring(0, datasourceUrl.lastIndexOf('/'));
		return baseUrl + "/postgres";
	}

	private String buildTenantJdbcUrl(String databaseName) {
		// Parse the original URL and replace database name with tenant database
		String baseUrl = datasourceUrl.substring(0, datasourceUrl.lastIndexOf('/'));
		return baseUrl + "/" + databaseName;
	}
}

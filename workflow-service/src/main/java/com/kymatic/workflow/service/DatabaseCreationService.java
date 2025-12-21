package com.kymatic.workflow.service;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for creating and migrating tenant databases.
 * Used by workflow-service to handle database creation as part of tenant onboarding.
 */
@Service
public class DatabaseCreationService {

	private static final Logger logger = LoggerFactory.getLogger(DatabaseCreationService.class);

	private final String baseUrl;
	private final String username;
	private final String password;
	private final String migrationLocations;

	public DatabaseCreationService(
		@Value("${tenant-database.base-url}") String baseUrl,
		@Value("${tenant-database.username}") String username,
		@Value("${tenant-database.password}") String password,
		@Value("${tenant-database.migration-locations:classpath:db/tenant}") String migrationLocations
	) {
		this.baseUrl = baseUrl;
		this.username = username;
		this.password = password;
		this.migrationLocations = migrationLocations;
	}

	/**
	 * Creates a tenant database if it doesn't exist.
	 */
	public void createDatabaseIfNotExists(String databaseName) {
		if (databaseExists(databaseName)) {
			logger.info("Tenant database '{}' already exists. Skipping creation.", databaseName);
			return;
		}

		logger.info("Creating tenant database: {}", databaseName);
		try (Connection connection = getConnection();
			 Statement statement = connection.createStatement()) {
			statement.execute("CREATE DATABASE \"" + databaseName + "\"");
			logger.info("Successfully created tenant database: {}", databaseName);
		} catch (Exception ex) {
			logger.error("Failed to create tenant database: {}", databaseName, ex);
			throw new RuntimeException("Failed to create tenant database " + databaseName, ex);
		}
	}

	/**
	 * Runs Flyway migrations on the tenant database.
	 */
	public List<String> migrateTenantDatabase(String databaseName) {
		String tenantJdbcUrl = buildTenantJdbcUrl(databaseName);
		logger.info("Running tenant migrations for database {}", tenantJdbcUrl);

		try {
			Flyway flyway = Flyway.configure()
				.dataSource(tenantJdbcUrl, username, password)
				.locations(migrationLocations)
				.baselineOnMigrate(true)
				.validateOnMigrate(true)
				.load();

			MigrateResult result = flyway.migrate();

			if (result.migrations == null || result.migrations.isEmpty()) {
				logger.warn("No migrations were applied to tenant database {}", databaseName);
				return List.of();
			}

			logger.info("Successfully applied {} migration(s) to tenant database {}", 
				result.migrations.size(), databaseName);

			return result.migrations.stream()
				.map(migration -> migration.version == null ? "baseline" : migration.version)
				.collect(Collectors.toList());
		} catch (Exception ex) {
			logger.error("Failed to migrate tenant database {}: {}", databaseName, ex.getMessage(), ex);
			throw new RuntimeException("Failed to migrate tenant database " + databaseName + ": " + ex.getMessage(), ex);
		}
	}

	private boolean databaseExists(String databaseName) {
		String postgresUrl = buildPostgresConnectionUrl();
		try (Connection connection = DriverManager.getConnection(postgresUrl, username, password);
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
			throw new RuntimeException("Failed to check if database exists: " + databaseName, ex);
		}
	}

	private Connection getConnection() throws Exception {
		String postgresUrl = buildPostgresConnectionUrl();
		return DriverManager.getConnection(postgresUrl, username, password);
	}

	private String buildPostgresConnectionUrl() {
		// Extract base URL and connect to 'postgres' database
		int idx = baseUrl.lastIndexOf('/');
		if (idx < 0) {
			throw new IllegalStateException("Unable to parse base URL: " + baseUrl);
		}
		String base = baseUrl.substring(0, idx + 1);
		return base + "postgres";
	}

	private String buildTenantJdbcUrl(String databaseName) {
		int idx = baseUrl.lastIndexOf('/');
		if (idx < 0) {
			throw new IllegalStateException("Unable to parse base URL: " + baseUrl);
		}
		return baseUrl.substring(0, idx + 1) + databaseName;
	}

	/**
	 * Builds the database connection string for a given database name.
	 * Used by delegates to set workflow variables.
	 */
	public String buildDatabaseConnectionString(String databaseName) {
		return buildTenantJdbcUrl(databaseName);
	}
}


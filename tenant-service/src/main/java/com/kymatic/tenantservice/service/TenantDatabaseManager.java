package com.kymatic.tenantservice.service;

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
			throw new IllegalStateException("Failed to check if database exists: " + databaseName, ex);
		}
	}

	private Connection getConnection() throws Exception {
		// Connect to 'postgres' database to create new databases
		// PostgreSQL doesn't allow creating databases while connected to the target database
		String postgresUrl = buildPostgresConnectionUrl();
		return java.sql.DriverManager.getConnection(postgresUrl, username, password);
	}

	private String buildPostgresConnectionUrl() {
		// Extract base URL and connect to 'postgres' database
		int idx = datasourceUrl.lastIndexOf('/');
		if (idx < 0) {
			throw new IllegalStateException("Unable to parse datasource URL: " + datasourceUrl);
		}
		String baseUrl = datasourceUrl.substring(0, idx + 1);
		return baseUrl + "postgres";
	}

	public String buildTenantJdbcUrl(String databaseName) {
		int idx = datasourceUrl.lastIndexOf('/');
		if (idx < 0) {
			throw new IllegalStateException("Unable to parse datasource URL: " + datasourceUrl);
		}
		return datasourceUrl.substring(0, idx + 1) + databaseName;
	}

	/**
	 * Execute SQL in tenant database using JDBC
	 */
	public void executeInTenantDatabase(String databaseName, String sql, Object... params) {
		String tenantJdbcUrl = buildTenantJdbcUrl(databaseName);
		logger.debug("Executing SQL in tenant database {}: {}", databaseName, sql);
		
		try (Connection connection = java.sql.DriverManager.getConnection(tenantJdbcUrl, username, password);
			 var preparedStatement = connection.prepareStatement(sql)) {
			
			// Set parameters
			for (int i = 0; i < params.length; i++) {
				preparedStatement.setObject(i + 1, params[i]);
			}
			
			preparedStatement.executeUpdate();
		} catch (Exception ex) {
			logger.error("Failed to execute SQL in tenant database {}: {}", databaseName, ex.getMessage(), ex);
			throw new IllegalStateException("Failed to execute SQL in tenant database " + databaseName + ": " + ex.getMessage(), ex);
		}
	}
}



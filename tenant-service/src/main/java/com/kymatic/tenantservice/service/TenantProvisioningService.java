package com.kymatic.tenantservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kymatic.tenantservice.dto.TenantRequest;
import com.kymatic.tenantservice.dto.TenantUserRequest;
import com.kymatic.tenantservice.persistence.entity.TenantEntity;
import com.kymatic.tenantservice.persistence.entity.TenantMigrationEntity;
import com.kymatic.tenantservice.persistence.repository.TenantMigrationRepository;
import com.kymatic.tenantservice.persistence.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class TenantProvisioningService {

	private static final Logger logger = LoggerFactory.getLogger(TenantProvisioningService.class);
	private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9\\-]+$");

	private final TenantRepository tenantRepository;
	private final TenantMigrationRepository tenantMigrationRepository;
	private final TenantDatabaseManager tenantDatabaseManager;
	private final TenantUserService tenantUserService;
	private final ObjectMapper objectMapper;

	public TenantProvisioningService(
		TenantRepository tenantRepository,
		TenantMigrationRepository tenantMigrationRepository,
		TenantDatabaseManager tenantDatabaseManager,
		TenantUserService tenantUserService,
		ObjectMapper objectMapper
	) {
		this.tenantRepository = tenantRepository;
		this.tenantMigrationRepository = tenantMigrationRepository;
		this.tenantDatabaseManager = tenantDatabaseManager;
		this.tenantUserService = tenantUserService;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public TenantEntity createTenant(TenantRequest request) {
		validateSlug(request.slug());
		tenantRepository.findBySlug(request.slug()).ifPresent(existing -> {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already in use: " + request.slug());
		});

		// Use slug as database name to match URL pattern (e.g., testcompany.localhost -> testcompany database)
		String databaseName = buildDatabaseNameFromSlug(request.slug());
		String jdbcUrl = buildTenantJdbcUrl(databaseName);

		TenantEntity tenant = new TenantEntity();
		tenant.setTenantName(request.tenantName());
		tenant.setSlug(request.slug());
		tenant.setSubscriptionTier(request.subscriptionTier());
		tenant.setDatabaseConnectionString(jdbcUrl);
		tenant.setDatabaseName(databaseName);
		tenant.setMaxUsers(request.maxUsers());
		tenant.setMaxStorageGb(request.maxStorageGb());
		tenant.setMetadata(nullSafeMetadata(request.metadata()));

		tenantDatabaseManager.createDatabaseIfNotExists(databaseName);
		List<String> appliedVersions = tenantDatabaseManager.migrateTenantDatabase(databaseName);

		TenantEntity saved = tenantRepository.save(tenant);
		recordMigrations(saved.getTenantId(), appliedVersions, "success");
		createDefaultTenantAdmin(saved, request.adminEmail());

		logger.info("Provisioned tenant {} with database {}", saved.getTenantId(), databaseName);
		return saved;
	}

	private void createDefaultTenantAdmin(TenantEntity tenant, String adminEmail) {
		// Generate default email if not provided: admin@{slug}.local
		String finalEmail;
		if (adminEmail == null || adminEmail.isBlank()) {
			finalEmail = "admin@" + tenant.getSlug() + ".local";
			logger.info("No admin email provided for tenant {}. Using default email: {}", tenant.getTenantName(), finalEmail);
		} else {
			finalEmail = adminEmail.trim().toLowerCase();
		}

		UUID userId = null;
		
		// Step 1: Create user in master database (tenant_users table) for authentication
		try {
			TenantUserRequest request = new TenantUserRequest(
				tenant.getTenantId().toString(),
				finalEmail,
				"adming",
				"admin",
				true
			);
			var userResponse = tenantUserService.createTenantUser(request);
			userId = userResponse.userId();
			logger.info("Created default tenant admin user {} in master database for tenant {} (password: adming)", finalEmail, tenant.getTenantName());
		} catch (Exception ex) {
			// If user already exists, try to get existing user ID
			if (ex.getMessage() != null && ex.getMessage().contains("already exists")) {
				logger.warn("Default admin user {} already exists in master database for tenant {}. Skipping creation.", finalEmail, tenant.getTenantName());
				// Try to get existing user ID
				try {
					var existingUser = tenantUserService.getTenantUserByEmail(tenant.getTenantId().toString(), finalEmail);
					userId = existingUser.userId();
				} catch (Exception e) {
					logger.error("Failed to retrieve existing user: {}", e.getMessage());
				}
			} else {
				logger.error("Failed to create default tenant admin in master database for {}: {}", tenant.getTenantName(), ex.getMessage(), ex);
				throw new IllegalStateException("Failed to create default admin user for tenant " + tenant.getTenantName(), ex);
			}
		}

		// Step 2: Create user in tenant-specific database (users table) for tenant data
		if (userId != null) {
			createUserInTenantDatabase(tenant, userId, finalEmail);
		}
	}

	private void createUserInTenantDatabase(TenantEntity tenant, UUID userId, String email) {
		try {
			String databaseName = tenant.getDatabaseName();
			String tenantSlug = tenant.getSlug();
			
			// Extract name from email (admin@example.com -> Admin)
			String firstName = "Admin";
			String lastName = "User";
			if (email.contains("@")) {
				String localPart = email.split("@")[0];
				if (!localPart.isEmpty()) {
					firstName = localPart.substring(0, 1).toUpperCase() + 
					           (localPart.length() > 1 ? localPart.substring(1) : "");
				}
			}
			
			// Insert user into tenant database users table
			// Note: tenant_id in tenant DB is stored as VARCHAR (slug), not UUID
			// Note: tenant DB users table doesn't have 'role' column (role is managed via RBAC tables)
			String insertSql = """
				INSERT INTO users (
					user_id, tenant_id, email, first_name, last_name, 
					is_active, email_verified, created_at, updated_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				ON CONFLICT (user_id) DO UPDATE SET
					email = EXCLUDED.email,
					first_name = EXCLUDED.first_name,
					last_name = EXCLUDED.last_name,
					is_active = EXCLUDED.is_active,
					email_verified = EXCLUDED.email_verified,
					updated_at = CURRENT_TIMESTAMP
				""";
			
			tenantDatabaseManager.executeInTenantDatabase(databaseName, insertSql, 
				userId, tenantSlug, email, firstName, lastName, true, true);
			
			logger.info("Created default admin user {} in tenant database {} for tenant {}", 
				email, databaseName, tenant.getTenantName());
		} catch (Exception ex) {
			// Log error but don't fail tenant creation if tenant DB user creation fails
			logger.warn("Failed to create user in tenant database {} for tenant {}: {}. " +
				"User was created in master database and can login. You may need to create user profile manually.",
				tenant.getDatabaseName(), tenant.getTenantName(), ex.getMessage());
		}
	}

	@Transactional(readOnly = true)
	public List<TenantEntity> listTenants() {
		return tenantRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Optional<TenantEntity> getTenant(UUID tenantId) {
		return tenantRepository.findById(tenantId);
	}

	@Transactional(readOnly = true)
	public Optional<TenantEntity> getTenantBySlug(String slug) {
		return tenantRepository.findBySlug(slug);
	}

	@Transactional
	public TenantEntity updateStatus(UUID tenantId, String status) {
		TenantEntity tenant = tenantRepository.findById(tenantId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found: " + tenantId));
		tenant.setStatus(status);
		return tenantRepository.save(tenant);
	}

	@Transactional
	public List<String> runMigrations(UUID tenantId) {
		TenantEntity tenant = tenantRepository.findById(tenantId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found: " + tenantId));

		List<String> versions = tenantDatabaseManager.migrateTenantDatabase(tenant.getDatabaseName());
		recordMigrations(tenantId, versions, "success");
		return versions;
	}

	@Transactional(readOnly = true)
	public List<TenantMigrationEntity> listMigrations(UUID tenantId) {
		return tenantMigrationRepository.findByTenantIdOrderByAppliedAtDesc(tenantId);
	}

	private void validateSlug(String slug) {
		if (!SLUG_PATTERN.matcher(slug).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid slug. Use lowercase letters, digits or hyphen.");
		}
	}

	/**
	 * Build database name from tenant slug to match URL pattern.
	 * 
	 * The database name is based on the tenant slug, matching the URL pattern:
	 * - Slug: "testcompany" → Database: "testcompany"
	 * - URL: http://testcompany.localhost:5173 → Database: testcompany
	 * 
	 * This ensures the database name matches the tenant slug, making dynamic routing straightforward.
	 * The slug is already validated (lowercase letters, digits, hyphens) and safe to use as database name.
	 */
	private String buildDatabaseNameFromSlug(String slug) {
		if (slug == null || slug.isBlank()) {
			throw new IllegalArgumentException("Slug cannot be null or empty");
		}
		
		// Slug is already validated (SLUG_PATTERN ensures lowercase letters, digits, hyphens)
		// Convert hyphens to underscores for PostgreSQL database name compatibility
		// PostgreSQL allows: letters, digits, underscores
		String databaseName = slug.toLowerCase()
			.replace('-', '_'); // Replace hyphens with underscores (PostgreSQL doesn't allow hyphens in unquoted names)
		
		// Ensure it's not empty and starts with a letter or underscore
		if (databaseName.isEmpty()) {
			throw new IllegalArgumentException("Invalid slug: results in empty database name");
		}
		
		// PostgreSQL database name must start with a letter or underscore
		// If it starts with a digit, prefix with 't_' to make it valid
		if (Character.isDigit(databaseName.charAt(0))) {
			databaseName = "t_" + databaseName;
			logger.warn("Database name prefixed with 't_' because slug starts with digit: {}", slug);
		}
		
		// PostgreSQL database name limit is 63 characters
		if (databaseName.length() > 63) {
			databaseName = databaseName.substring(0, 63);
			logger.warn("Database name truncated to 63 characters for slug: {}", slug);
		}
		
		logger.info("Database name for slug '{}': '{}'", slug, databaseName);
		return databaseName;
	}

	private String buildTenantJdbcUrl(String databaseName) {
		return tenantDatabaseManager.buildTenantJdbcUrl(databaseName);
	}

	private void recordMigrations(UUID tenantId, List<String> versions, String status) {
		for (String version : versions) {
			if (version == null || version.isBlank()) {
				continue;
			}
			if (tenantMigrationRepository.existsByTenantIdAndVersion(tenantId, version)) {
				continue;
			}
			TenantMigrationEntity entity = new TenantMigrationEntity();
			entity.setTenantId(tenantId);
			entity.setVersion(version);
			entity.setStatus(status);
			entity.setAppliedAt(OffsetDateTime.now());
			tenantMigrationRepository.save(entity);
		}
	}

	private JsonNode nullSafeMetadata(JsonNode metadata) {
		if (metadata == null) {
			return objectMapper.createObjectNode();
		}
		return metadata;
	}
}



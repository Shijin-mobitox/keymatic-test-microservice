package com.kymatic.tenantservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kymatic.tenantservice.client.KeycloakClientWrapper;
import com.kymatic.tenantservice.dto.CreateTenantRequest;
import com.kymatic.tenantservice.dto.CreateUserRequest;
import com.kymatic.tenantservice.dto.TenantOnboardingResponse;
import com.kymatic.tenantservice.dto.UserOnboardingResponse;
import com.kymatic.tenantservice.exception.KeycloakException;
import com.kymatic.tenantservice.exception.OrganizationAlreadyExistsException;
import com.kymatic.tenantservice.exception.TenantOnboardingException;
import com.kymatic.tenantservice.exception.UserAlreadyExistsException;
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
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service for tenant onboarding with Keycloak Organizations integration.
 * 
 * Orchestrates the complete tenant onboarding flow:
 * 1. Create organization in Keycloak
 * 2. Create and initialize tenant database
 * 3. Create initial admin user in Keycloak
 * 4. Assign user to organization
 * 5. Assign roles to user
 * 
 * Includes rollback/compensation logic for failed operations.
 */
@Service
public class TenantOnboardingService {

	private static final Logger logger = LoggerFactory.getLogger(TenantOnboardingService.class);
	private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9\\-]+$");

	private final TenantRepository tenantRepository;
	private final TenantMigrationRepository tenantMigrationRepository;
	private final TenantDatabaseManager tenantDatabaseManager;
	private final KeycloakClientWrapper keycloakClientWrapper;
	private final ObjectMapper objectMapper;

	public TenantOnboardingService(
		TenantRepository tenantRepository,
		TenantMigrationRepository tenantMigrationRepository,
		TenantDatabaseManager tenantDatabaseManager,
		KeycloakClientWrapper keycloakClientWrapper,
		ObjectMapper objectMapper
	) {
		this.tenantRepository = tenantRepository;
		this.tenantMigrationRepository = tenantMigrationRepository;
		this.tenantDatabaseManager = tenantDatabaseManager;
		this.keycloakClientWrapper = keycloakClientWrapper;
		this.objectMapper = objectMapper;
	}

	/**
	 * Creates a new tenant with Keycloak Organizations integration.
	 * 
	 * Flow:
	 * 1. Validate input
	 * 2. Create organization in Keycloak
	 * 3. Create tenant database
	 * 4. Run database migrations
	 * 5. Save tenant record in master database
	 * 6. Create admin user in Keycloak
	 * 7. Assign user to organization
	 * 8. Assign admin role to user
	 * 
	 * If any step fails, rollback is performed.
	 */
	@Transactional
	public TenantOnboardingResponse createTenant(CreateTenantRequest request) {
		logger.info("Starting tenant onboarding: slug={}, name={}", request.slug(), request.tenantName());

		// Validate slug
		validateSlug(request.slug());

		// Handle adminUser - support both new format (adminUser object) and legacy format (adminEmail string)
		CreateTenantRequest.AdminUserRequest adminUser = request.adminUser();
		if (adminUser == null) {
			// Legacy format: create adminUser from adminEmail or generate default
			String adminEmail = request.adminEmail();
			if (adminEmail == null || adminEmail.isBlank()) {
				// Generate default email from slug
				adminEmail = "admin@" + request.slug() + ".local";
				logger.info("No admin email provided, generating default: {}", adminEmail);
			}
			
			// Create default adminUser with generated/default email
			adminUser = new CreateTenantRequest.AdminUserRequest(
				adminEmail,
				"TempPassword123!", // Default password - should be changed on first login
				"Admin",
				"User",
				true
			);
			logger.info("Using legacy format or defaults - created adminUser with email: {}", adminEmail);
		}

		// Check if tenant already exists in database
		tenantRepository.findBySlug(request.slug()).ifPresent(existing -> {
			throw new ResponseStatusException(HttpStatus.CONFLICT, 
				"Tenant with slug '" + request.slug() + "' already exists");
		});

		// Use Groups instead of Organizations (stable approach for all Keycloak versions)
		String keycloakGroupId = null;
		String adminUserId = null;
		TenantEntity tenantEntity = null;

		try {
			// Step 1: Create group in Keycloak (using Groups instead of Organizations)
			logger.info("Step 1: Creating group in Keycloak: name={}", request.slug());
			keycloakGroupId = keycloakClientWrapper.createGroup(request.slug(), "Tenant group for " + request.tenantName());
			logger.info("Group created in Keycloak: id={}, name={}", keycloakGroupId, request.slug());

			// Step 2: Create tenant database
			logger.info("Step 2: Creating tenant database: slug={}", request.slug());
			String databaseName = buildDatabaseNameFromSlug(request.slug());
			String jdbcUrl = buildTenantJdbcUrl(databaseName);

			tenantDatabaseManager.createDatabaseIfNotExists(databaseName);
			logger.info("Database created: {}", databaseName);

			// Step 3: Run database migrations
			logger.info("Step 3: Running database migrations: database={}", databaseName);
			List<String> appliedVersions = tenantDatabaseManager.migrateTenantDatabase(databaseName);
			logger.info("Migrations applied: versions={}", appliedVersions);

			// Step 4: Save tenant record in master database
			logger.info("Step 4: Saving tenant record in master database");
			tenantEntity = new TenantEntity();
			tenantEntity.setTenantName(request.tenantName());
			tenantEntity.setSlug(request.slug());
			tenantEntity.setSubscriptionTier(request.subscriptionTier());
			tenantEntity.setDatabaseConnectionString(jdbcUrl);
			tenantEntity.setDatabaseName(databaseName);
			tenantEntity.setMaxUsers(request.maxUsers());
			tenantEntity.setMaxStorageGb(request.maxStorageGb());
			tenantEntity.setMetadata(nullSafeMetadata(request.metadata()));
			tenantEntity.setStatus("active");

			TenantEntity saved = tenantRepository.save(tenantEntity);
			recordMigrations(saved.getTenantId(), appliedVersions, "success");
			logger.info("Tenant record saved: id={}", saved.getTenantId());

			// Step 5: Create admin user in Keycloak
			logger.info("Step 5: Creating admin user in Keycloak: email={}", adminUser.email());
			adminUserId = keycloakClientWrapper.createUser(
				adminUser.email(),
				adminUser.password(),
				adminUser.firstName(),
				adminUser.lastName(),
				adminUser.emailVerified()
			);
			logger.info("Admin user created in Keycloak: id={}, email={}", adminUserId, adminUser.email());

			// Wait for Keycloak to fully index the user before organization assignment
			// This helps prevent "User does not exist" errors during assignment
			// Increased wait time based on observed timing issues
			try {
				logger.info("Waiting 8 seconds for Keycloak to fully index user before organization assignment...");
				Thread.sleep(8000);
				
				// Additional verification that user exists before attempting assignment
				logger.info("Verifying user exists in Keycloak before organization assignment: userId={}", adminUserId);
				boolean userExists = keycloakClientWrapper.verifyUserExists(adminUserId);
				if (!userExists) {
					throw new KeycloakException("User verification failed: User does not exist in Keycloak with ID: " + adminUserId);
				}
				logger.info("User verification successful, proceeding with organization assignment");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new KeycloakException("Interrupted while waiting for user indexing", e);
			}

			// Step 6: Assign user to group (using Groups instead of Organizations)
			logger.info("Step 6: Assigning user to group: userId={}, groupId={}", adminUserId, keycloakGroupId);
			keycloakClientWrapper.assignUserToGroup(keycloakGroupId, adminUserId);
			logger.info("User assigned to group successfully");

			// Step 7: Assign admin role to user (optional - skip for Groups approach)
			// Note: Role assignment can be handled separately if needed
			logger.info("Step 7: Skipping role assignment (using Groups approach)");

			logger.info("Tenant onboarding completed successfully: slug={}, tenantId={}, groupId={}", 
				request.slug(), saved.getTenantId(), keycloakGroupId);

			return new TenantOnboardingResponse(
				saved.getTenantId(),
				saved.getTenantName(),
				saved.getSlug(),
				keycloakGroupId, // Using groupId instead of orgId
				request.slug(),
				saved.getDatabaseName(),
				saved.getDatabaseConnectionString(),
				saved.getStatus(),
				adminUserId,
				adminUser.email(),
				saved.getCreatedAt(),
				saved.getMetadata()
			);

		} catch (OrganizationAlreadyExistsException | UserAlreadyExistsException | ResponseStatusException e) {
			// These are expected exceptions - don't rollback, just rethrow
			throw e;
		} catch (Exception e) {
			logger.error("Tenant onboarding failed: slug={}", request.slug(), e);
			
			// Rollback/compensation logic
			performRollback(keycloakGroupId, adminUserId, tenantEntity, request.slug());
			
			// Determine which step failed
			TenantOnboardingException.OnboardingStep failedStep = determineFailedStep(
				keycloakGroupId, adminUserId, tenantEntity);
			
			throw new TenantOnboardingException(
				request.slug(),
				failedStep,
				"Tenant onboarding failed: " + e.getMessage(),
				e
			);
		}
	}

	/**
	 * Creates a new user under a tenant (organization).
	 * 
	 * This method is temporarily disabled while we focus on the main tenant creation flow.
	 * Use the main tenant creation endpoint with adminUser instead.
	 */
	@Transactional(readOnly = true)
	public UserOnboardingResponse createUserForTenant(String tenantAlias, CreateUserRequest request) {
		logger.info("createUserForTenant called for tenant: {}, email: {}", tenantAlias, request.email());
		
		// This method is temporarily disabled to focus on main tenant creation
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, 
			"createUserForTenant method is temporarily disabled. Use main tenant creation with adminUser instead.");
	}

	/**
	 * Performs rollback/compensation for failed tenant onboarding.
	 */
	private void performRollback(String keycloakGroupId, String adminUserId, TenantEntity tenantEntity, String slug) {
		logger.info("Performing rollback for failed tenant onboarding: slug={}", slug);

		// Rollback in reverse order of creation

		// 1. Delete tenant record from database (if created)
		if (tenantEntity != null && tenantEntity.getTenantId() != null) {
			try {
				logger.info("Rollback: Deleting tenant record from database: tenantId={}", tenantEntity.getTenantId());
				tenantRepository.delete(tenantEntity);
				logger.info("Rollback: Tenant record deleted");
			} catch (Exception e) {
				logger.error("Rollback: Failed to delete tenant record: tenantId={}", tenantEntity.getTenantId(), e);
			}
		}

		// 2. Delete user from Keycloak (if created)
		if (adminUserId != null) {
			try {
				logger.info("Rollback: Deleting user from Keycloak: userId={}", adminUserId);
				keycloakClientWrapper.deleteUser(adminUserId);
				logger.info("Rollback: User deleted from Keycloak");
			} catch (Exception e) {
				logger.error("Rollback: Failed to delete user from Keycloak: userId={}", adminUserId, e);
			}
		}

		// 3. Delete group from Keycloak (if created)
		if (keycloakGroupId != null && !keycloakGroupId.equals("SKIPPED")) {
			try {
				logger.info("Rollback: Deleting group from Keycloak: groupId={}", keycloakGroupId);
				keycloakClientWrapper.deleteGroup(keycloakGroupId);
				logger.info("Rollback: Group deleted from Keycloak");
			} catch (Exception e) {
				logger.error("Rollback: Failed to delete group from Keycloak: groupId={}", keycloakGroupId, e);
			}
		}

		// Note: Database deletion is not rolled back automatically
		// The database may remain but will be orphaned. Manual cleanup may be required.
		logger.warn("Rollback: Tenant database '{}' may need manual cleanup", 
			tenantEntity != null ? tenantEntity.getDatabaseName() : "unknown");
	}

	/**
	 * Determines which step failed during onboarding.
	 */
	private TenantOnboardingException.OnboardingStep determineFailedStep(
		String keycloakGroupId, String adminUserId, TenantEntity tenantEntity) {
		
		if (keycloakGroupId == null) {
			return TenantOnboardingException.OnboardingStep.KEYCLOAK_ORG_CREATION; // Keep same enum for compatibility
		}
		if (tenantEntity == null || tenantEntity.getDatabaseName() == null) {
			return TenantOnboardingException.OnboardingStep.DATABASE_CREATION;
		}
		if (adminUserId == null) {
			return TenantOnboardingException.OnboardingStep.USER_CREATION;
		}
		return TenantOnboardingException.OnboardingStep.USER_ORG_ASSIGNMENT; // Keep same enum for compatibility
	}

	private void validateSlug(String slug) {
		if (!SLUG_PATTERN.matcher(slug).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
				"Invalid slug. Use lowercase letters, digits or hyphen.");
		}
	}

	private String buildDatabaseNameFromSlug(String slug) {
		if (slug == null || slug.isBlank()) {
			throw new IllegalArgumentException("Slug cannot be null or empty");
		}
		
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


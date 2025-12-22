package com.kymatic.tenantservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kymatic.tenantservice.client.KeycloakClientWrapper;
import com.kymatic.tenantservice.dto.CreateTenantRequest;
import com.kymatic.tenantservice.dto.CreateUserRequest;
import com.kymatic.tenantservice.dto.TenantOnboardingResponse;
import com.kymatic.tenantservice.dto.UserOnboardingResponse;
import com.kymatic.tenantservice.exception.OrganizationAlreadyExistsException;
import com.kymatic.tenantservice.exception.TenantOnboardingException;
import com.kymatic.tenantservice.exception.UserAlreadyExistsException;
import com.kymatic.tenantservice.persistence.entity.TenantEntity;
import com.kymatic.tenantservice.persistence.entity.TenantMigrationEntity;
import com.kymatic.tenantservice.persistence.repository.TenantMigrationRepository;
import com.kymatic.tenantservice.persistence.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
	
	@Value("${keycloak.admin.organization-cleanup-enabled:false}")
	private final boolean organizationCleanupEnabled;

	public TenantOnboardingService(
		TenantRepository tenantRepository,
		TenantMigrationRepository tenantMigrationRepository,
		TenantDatabaseManager tenantDatabaseManager,
		KeycloakClientWrapper keycloakClientWrapper,
		ObjectMapper objectMapper,
		@Value("${keycloak.admin.organization-cleanup-enabled:false}") boolean organizationCleanupEnabled
	) {
		this.tenantRepository = tenantRepository;
		this.tenantMigrationRepository = tenantMigrationRepository;
		this.tenantDatabaseManager = tenantDatabaseManager;
		this.keycloakClientWrapper = keycloakClientWrapper;
		this.objectMapper = objectMapper;
		this.organizationCleanupEnabled = organizationCleanupEnabled;
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

		// Use Organizations feature (following Skycloak best practices for Keycloak 26.2.4)
		String keycloakOrgId = null;
		String adminUserId = null;
		TenantEntity tenantEntity = null;

		try {
			// Step 1: Create admin user in Keycloak first (required for organization membership)
			logger.info("Step 1: Creating admin user in Keycloak: email={}", adminUser.email());
			adminUserId = keycloakClientWrapper.createUser(
				adminUser.email(),
				adminUser.password(),
				adminUser.firstName(),
				adminUser.lastName(),
				adminUser.emailVerified()
			);
			logger.info("Admin user created in Keycloak: id={}, email={}", adminUserId, adminUser.email());

			// Step 2: Create organization with user as member (atomic operation - no delays needed!)
			logger.info("Step 2: Creating organization with user as member: alias={}", request.slug());
			if (organizationCleanupEnabled) {
				logger.info("Organization cleanup enabled - using cleanup method with user membership for: {}", request.slug());
				keycloakOrgId = keycloakClientWrapper.createOrganizationWithCleanupAndUser(request.slug(), request.tenantName(), adminUserId);
			} else {
				keycloakOrgId = keycloakClientWrapper.createOrganizationWithUser(request.slug(), request.tenantName(), adminUserId);
			}
			logger.info("Organization created with user membership: id={}, alias={}, adminUserId={}", keycloakOrgId, request.slug(), adminUserId);

			// Step 3: Create tenant database
			logger.info("Step 3: Creating tenant database: slug={}", request.slug());
			String databaseName = buildDatabaseNameFromSlug(request.slug());
			String jdbcUrl = buildTenantJdbcUrl(databaseName);

			tenantDatabaseManager.createDatabaseIfNotExists(databaseName);
			logger.info("Database created: {}", databaseName);

			// Step 4: Run database migrations
			logger.info("Step 4: Running database migrations: database={}", databaseName);
			List<String> appliedVersions = tenantDatabaseManager.migrateTenantDatabase(databaseName);
			logger.info("Migrations applied: versions={}", appliedVersions);

			// Step 5: Save tenant record in master database
			logger.info("Step 5: Saving tenant record in master database");
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

			// Note: User is already assigned to organization during creation - no delays or separate assignment needed!
			logger.info("✅ User-organization assignment completed atomically during creation");

			logger.info("Tenant onboarding completed successfully: slug={}, tenantId={}, orgId={}", 
				request.slug(), saved.getTenantId(), keycloakOrgId);

			return new TenantOnboardingResponse(
				saved.getTenantId(),
				saved.getTenantName(),
				saved.getSlug(),
				keycloakOrgId, // Using proper organizationId
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
			performRollback(keycloakOrgId, adminUserId, tenantEntity, request.slug());
			
			// Determine which step failed
			TenantOnboardingException.OnboardingStep failedStep = determineFailedStep(
				keycloakOrgId, adminUserId, tenantEntity);
			
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
	private void performRollback(String keycloakOrgId, String adminUserId, TenantEntity tenantEntity, String slug) {
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

		// 3. Delete organization from Keycloak (if created)
		if (keycloakOrgId != null) {
			try {
				logger.info("Rollback: Deleting organization from Keycloak: orgId={}", keycloakOrgId);
				keycloakClientWrapper.deleteOrganization(keycloakOrgId);
				logger.info("Rollback: Organization deleted from Keycloak");
			} catch (Exception e) {
				logger.error("Rollback: Failed to delete organization from Keycloak: orgId={}", keycloakOrgId, e);
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
		String keycloakOrgId, String adminUserId, TenantEntity tenantEntity) {
		
		if (keycloakOrgId == null) {
			return TenantOnboardingException.OnboardingStep.KEYCLOAK_ORG_CREATION;
		}
		if (tenantEntity == null || tenantEntity.getDatabaseName() == null) {
			return TenantOnboardingException.OnboardingStep.DATABASE_CREATION;
		}
		if (adminUserId == null) {
			return TenantOnboardingException.OnboardingStep.USER_CREATION;
		}
		return TenantOnboardingException.OnboardingStep.USER_ORG_ASSIGNMENT;
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


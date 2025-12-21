package com.kymatic.tenantservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kymatic.tenantservice.exception.KeycloakException;
import com.kymatic.tenantservice.exception.OrganizationAlreadyExistsException;
import com.kymatic.tenantservice.exception.UserAlreadyExistsException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Wrapper for Keycloak Admin Client and REST API to manage Organizations and Users.
 * 
 * This component provides methods to:
 * - Create organizations (tenants) in Keycloak via REST API
 * - Create users in Keycloak via Admin Client
 * - Assign users to organizations via REST API
 * - Assign roles to users via REST API
 * 
 * Uses Keycloak Organizations feature (available in Keycloak >= 26).
 * All operations use a single realm with multiple organizations as tenant boundaries.
 * 
 * Note: Organizations API is accessed via REST API as Admin Client may not fully support it in v26.
 */
@Component
public class KeycloakClientWrapper {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakClientWrapper.class);

	private final String serverUrl;
	private final String realm;
	private final String clientId;
	private final String clientSecret;
	private final String username;
	private final String password;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public KeycloakClientWrapper(
		@Value("${keycloak.admin.server-url}") String serverUrl,
		@Value("${keycloak.admin.realm}") String realm,
		@Value("${keycloak.admin.client-id}") String clientId,
		@Value("${keycloak.admin.client-secret:}") String clientSecret,
		@Value("${keycloak.admin.username}") String username,
		@Value("${keycloak.admin.password}") String password,
		ObjectMapper objectMapper
	) {
		this.serverUrl = serverUrl;
		this.realm = realm;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.username = username;
		this.password = password;
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
	}

	/**
	 * Creates a Keycloak admin client instance.
	 * Uses password grant type for authentication.
	 * For admin operations (like creating organizations), authenticates against master realm.
	 */
	private Keycloak getKeycloakAdminClient() {
		KeycloakBuilder builder = KeycloakBuilder.builder()
			.serverUrl(serverUrl)
			.realm(realm)
			.clientId(clientId)
			.username(username)
			.password(password);

		// Add client secret if provided (for client credentials grant)
		if (clientSecret != null && !clientSecret.isBlank()) {
			builder.clientSecret(clientSecret);
		}

		return builder.build();
	}

	/**
	 * Gets an admin access token for REST API calls.
	 * Authenticates against master realm to get full admin privileges.
	 */
	private String getAdminAccessToken() {
		try {
			// For admin operations, authenticate against master realm
			KeycloakBuilder builder = KeycloakBuilder.builder()
				.serverUrl(serverUrl)
				.realm("master")  // Use master realm for admin operations
				.clientId("admin-cli")  // admin-cli is in master realm
				.username(username)
				.password(password);

			Keycloak keycloak = builder.build();
			return keycloak.tokenManager().getAccessTokenString();
		} catch (Exception e) {
			logger.error("Failed to get admin access token", e);
			throw new KeycloakException("Failed to authenticate with Keycloak: " + e.getMessage(), e);
		}
	}

	/**
	 * Creates a new organization in Keycloak via REST API.
	 * 
	 * @param alias Organization alias (URL-friendly, used as tenant identifier)
	 * @param name Organization display name
	 * @return Organization ID
	 * @throws OrganizationAlreadyExistsException if organization with alias already exists
	 * @throws KeycloakException if Keycloak API call fails
	 */
	public String createOrganization(String alias, String name) {
		logger.info("Creating organization in Keycloak: alias={}, name={}", alias, name);

		// Check if organization already exists
		if (organizationExists(alias)) {
			throw new OrganizationAlreadyExistsException(alias);
		}

		try {
			String accessToken = getAdminAccessToken();
			String orgsUrl = String.format("%s/admin/realms/%s/organizations", serverUrl, realm);

			// Generate domain from alias (e.g., "testtenant1759" -> "testtenant1759.local")
			String domain = alias + ".local";
			
			// Create organization JSON with required domain using ObjectMapper for proper serialization
			// Keycloak requires at least one domain when creating an organization
			JsonNode domainNode = objectMapper.createObjectNode()
				.put("name", domain);
			JsonNode domainsArray = objectMapper.createArrayNode().add(domainNode);
			JsonNode orgNode = objectMapper.createObjectNode()
				.put("alias", alias)
				.put("name", name)
				.put("enabled", true)
				.set("domains", domainsArray);
			
			String orgJson = objectMapper.writeValueAsString(orgNode);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(orgsUrl))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(orgJson))
				.timeout(Duration.ofSeconds(30))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 201) {
				// Extract organization ID from Location header or response body
				String location = response.headers().firstValue("Location").orElse("");
				String orgId = location.isEmpty() ? extractOrgIdFromResponse(response.body()) : 
					location.substring(location.lastIndexOf('/') + 1);
				
				logger.info("Successfully created organization in Keycloak: alias={}, id={}", alias, orgId);
				return orgId;
			} else if (response.statusCode() == 409) {
				logger.warn("Organization with alias '{}' already exists in Keycloak", alias);
				throw new OrganizationAlreadyExistsException(alias);
			} else {
				String errorMessage = String.format("Failed to create organization in Keycloak. Status: %d, Response: %s",
					response.statusCode(), response.body());
				logger.error(errorMessage);
				throw new KeycloakException(errorMessage);
			}
		} catch (OrganizationAlreadyExistsException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Error creating organization in Keycloak: alias={}", alias, e);
			throw new KeycloakException("Failed to create organization in Keycloak: " + e.getMessage(), e);
		}
	}

	/**
	 * Extracts organization ID from JSON response.
	 */
	private String extractOrgIdFromResponse(String responseBody) {
		try {
			JsonNode json = objectMapper.readTree(responseBody);
			if (json.has("id")) {
				return json.get("id").asText();
			}
		} catch (Exception e) {
			logger.warn("Failed to extract org ID from response: {}", responseBody);
		}
		return null;
	}

	/**
	 * Checks if an organization with the given alias exists.
	 */
	public boolean organizationExists(String alias) {
		try {
			String accessToken = getAdminAccessToken();
			String orgsUrl = String.format("%s/admin/realms/%s/organizations?alias=%s", serverUrl, realm, alias);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(orgsUrl))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.timeout(Duration.ofSeconds(10))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200) {
				JsonNode json = objectMapper.readTree(response.body());
				return json.isArray() && json.size() > 0;
			}
			return false;
		} catch (Exception e) {
			logger.warn("Error checking if organization exists: alias={}", alias, e);
			return false;
		}
	}

	/**
	 * Gets organization by alias.
	 */
	public Optional<OrganizationInfo> getOrganizationByAlias(String alias) {
		try {
			String accessToken = getAdminAccessToken();
			String orgsUrl = String.format("%s/admin/realms/%s/organizations?alias=%s", serverUrl, realm, alias);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(orgsUrl))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.timeout(Duration.ofSeconds(10))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200) {
				JsonNode json = objectMapper.readTree(response.body());
				if (json.isArray() && json.size() > 0) {
					JsonNode org = json.get(0);
					return Optional.of(new OrganizationInfo(
						org.get("id").asText(),
						org.get("alias").asText(),
						org.has("name") ? org.get("name").asText() : null
					));
				}
			}
			return Optional.empty();
		} catch (Exception e) {
			logger.warn("Error getting organization by alias: alias={}", alias, e);
			return Optional.empty();
		}
	}

	/**
	 * Simple data class for organization info.
	 */
	public static class OrganizationInfo {
		private final String id;
		private final String alias;
		private final String name;

		public OrganizationInfo(String id, String alias, String name) {
			this.id = id;
			this.alias = alias;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public String getAlias() {
			return alias;
		}

		public String getName() {
			return name;
		}
	}

	/**
	 * Creates a new user in Keycloak.
	 * 
	 * @param email User email (used as username)
	 * @param password User password
	 * @param firstName User first name
	 * @param lastName User last name
	 * @param emailVerified Whether email is verified
	 * @return User ID
	 * @throws UserAlreadyExistsException if user with email already exists
	 * @throws KeycloakException if Keycloak API call fails
	 */
	public String createUser(String email, String password, String firstName, String lastName, boolean emailVerified) {
		logger.info("Creating user in Keycloak: email={}, realm={}", email, realm);

		try {
			// Get admin access token
			String accessToken = getAdminAccessToken();
			
			// Check if user already exists
			String searchUrl = String.format("%s/admin/realms/%s/users?email=%s&exact=true", serverUrl, realm, email);
			HttpRequest searchRequest = HttpRequest.newBuilder()
				.uri(URI.create(searchUrl))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.timeout(Duration.ofSeconds(10))
				.build();

			HttpResponse<String> searchResponse = httpClient.send(searchRequest, HttpResponse.BodyHandlers.ofString());
			if (searchResponse.statusCode() == 200) {
				JsonNode users = objectMapper.readTree(searchResponse.body());
				if (users.isArray() && users.size() > 0) {
					throw new UserAlreadyExistsException(email);
				}
			}

			// Create user via REST API
			String usersUrl = String.format("%s/admin/realms/%s/users", serverUrl, realm);
			
			// Create user JSON
			JsonNode userNode = objectMapper.createObjectNode()
				.put("username", email)
				.put("email", email)
				.put("firstName", firstName)
				.put("lastName", lastName)
				.put("emailVerified", emailVerified)
				.put("enabled", true);
			
			String userJson = objectMapper.writeValueAsString(userNode);

			HttpRequest createRequest = HttpRequest.newBuilder()
				.uri(URI.create(usersUrl))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(userJson))
				.timeout(Duration.ofSeconds(30))
				.build();

			HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());

			if (createResponse.statusCode() == 201) {
				// Extract user ID from Location header
				String location = createResponse.headers().firstValue("Location").orElse("");
				String userId = location.isEmpty() ? null : location.substring(location.lastIndexOf('/') + 1);
				
				// If we couldn't get it from Location header, search for the user by email
				if (userId == null || userId.isEmpty()) {
					logger.debug("User ID not in Location header, searching by email: {}", email);
					// Wait a moment for user to be available
					Thread.sleep(500);
					
					HttpResponse<String> getUserResponse = httpClient.send(searchRequest, HttpResponse.BodyHandlers.ofString());
					if (getUserResponse.statusCode() == 200) {
						JsonNode users = objectMapper.readTree(getUserResponse.body());
						if (users.isArray() && users.size() > 0) {
							userId = users.get(0).get("id").asText();
							logger.debug("Found user ID by email search: {}", userId);
						}
					}
				}

				if (userId == null || userId.isEmpty()) {
					throw new KeycloakException("Failed to extract user ID after creation. Location: " + location);
				}
				
				// Verify user exists before proceeding
				if (!verifyUserExists(userId, accessToken)) {
					throw new KeycloakException("User was created but cannot be found: " + userId);
				}

				// Set password
				String passwordUrl = String.format("%s/admin/realms/%s/users/%s/reset-password", serverUrl, realm, userId);
				JsonNode passwordNode = objectMapper.createObjectNode()
					.put("type", "password")
					.put("value", password)
					.put("temporary", false);
				String passwordJson = objectMapper.writeValueAsString(passwordNode);

				HttpRequest passwordRequest = HttpRequest.newBuilder()
					.uri(URI.create(passwordUrl))
					.header("Authorization", "Bearer " + accessToken)
					.header("Content-Type", "application/json")
					.PUT(HttpRequest.BodyPublishers.ofString(passwordJson))
					.timeout(Duration.ofSeconds(30))
					.build();

				HttpResponse<String> passwordResponse = httpClient.send(passwordRequest, HttpResponse.BodyHandlers.ofString());

				if (passwordResponse.statusCode() >= 200 && passwordResponse.statusCode() < 300) {
					logger.info("Successfully created user in Keycloak: email={}, id={}", email, userId);
					// Verify user exists one more time before returning
					if (!verifyUserExists(userId, accessToken)) {
						throw new KeycloakException("User was created but cannot be verified: " + userId);
					}
					return userId;
				} else {
					logger.warn("User created but password setting failed: status={}, response={}", 
						passwordResponse.statusCode(), passwordResponse.body());
					// Verify user exists even if password setting failed
					if (!verifyUserExists(userId, accessToken)) {
						throw new KeycloakException("User was created but cannot be verified: " + userId);
					}
					// User was created, so return the ID even if password setting failed
					return userId;
				}
			} else if (createResponse.statusCode() == 409) {
				logger.warn("User with email '{}' already exists in Keycloak", email);
				throw new UserAlreadyExistsException(email);
			} else {
				String errorMessage = String.format("Failed to create user in Keycloak. Status: %d, Response: %s",
					createResponse.statusCode(), createResponse.body());
				logger.error(errorMessage);
				throw new KeycloakException(errorMessage);
			}
		} catch (UserAlreadyExistsException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Error creating user in Keycloak: email={}, error={}", email, e.getMessage(), e);
			throw new KeycloakException("Failed to create user in Keycloak: " + e.getMessage(), e);
		}
	}

	/**
	 * Public method to verify that a user exists in Keycloak.
	 * Used by external services to check user existence before operations.
	 */
	public boolean verifyUserExists(String userId) {
		try {
			String accessToken = getAdminAccessToken();
			return verifyUserExists(userId, accessToken);
		} catch (Exception e) {
			logger.warn("Failed to verify user exists: userId={}, error={}", userId, e.getMessage());
			return false;
		}
	}

	/**
	 * Creates a group in Keycloak (alternative to Organizations).
	 * Groups are more stable than Organizations in Keycloak 26.2.0.
	 */
	public String createGroup(String groupName, String description) {
		logger.info("Creating group in Keycloak: name={}, realm={}", groupName, realm);

		try {
			String accessToken = getAdminAccessToken();
			
			// Check if group already exists
			String searchUrl = String.format("%s/admin/realms/%s/groups?search=%s", serverUrl, realm, groupName);
			HttpRequest searchRequest = HttpRequest.newBuilder()
				.uri(URI.create(searchUrl))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.timeout(Duration.ofSeconds(10))
				.build();

			HttpResponse<String> searchResponse = httpClient.send(searchRequest, HttpResponse.BodyHandlers.ofString());
			if (searchResponse.statusCode() == 200) {
				JsonNode groups = objectMapper.readTree(searchResponse.body());
				if (groups.isArray() && groups.size() > 0) {
					for (JsonNode group : groups) {
						if (groupName.equals(group.get("name").asText())) {
							String existingGroupId = group.get("id").asText();
							logger.info("Group already exists in Keycloak: name={}, id={}", groupName, existingGroupId);
							return existingGroupId;
						}
					}
				}
			}

			// Create group via REST API
			String groupsUrl = String.format("%s/admin/realms/%s/groups", serverUrl, realm);
			
			// Create group JSON - simplified format for Keycloak 25.0.0
			JsonNode groupNode = objectMapper.createObjectNode()
				.put("name", groupName);
			
			// Note: Removed path and attributes for compatibility with Keycloak 25.0.0
			
			String groupJson = objectMapper.writeValueAsString(groupNode);

			HttpRequest createRequest = HttpRequest.newBuilder()
				.uri(URI.create(groupsUrl))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(groupJson))
				.timeout(Duration.ofSeconds(30))
				.build();

			HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());

			if (createResponse.statusCode() == 201) {
				// Extract group ID from Location header
				String location = createResponse.headers().firstValue("Location").orElse("");
				String groupId = location.isEmpty() ? null : location.substring(location.lastIndexOf('/') + 1);
				
				if (groupId == null || groupId.isEmpty()) {
					// Fallback: search for the group by name
					Thread.sleep(500);
					HttpResponse<String> getGroupResponse = httpClient.send(searchRequest, HttpResponse.BodyHandlers.ofString());
					if (getGroupResponse.statusCode() == 200) {
						JsonNode groups = objectMapper.readTree(getGroupResponse.body());
						if (groups.isArray() && groups.size() > 0) {
							for (JsonNode group : groups) {
								if (groupName.equals(group.get("name").asText())) {
									groupId = group.get("id").asText();
									break;
								}
							}
						}
					}
				}

				if (groupId == null || groupId.isEmpty()) {
					throw new KeycloakException("Failed to extract group ID after creation. Location: " + location);
				}

				logger.info("Successfully created group in Keycloak: name={}, id={}", groupName, groupId);
				return groupId;
			} else if (createResponse.statusCode() == 409) {
				logger.warn("Group with name '{}' already exists in Keycloak", groupName);
				// Try to get the existing group ID
				HttpResponse<String> getGroupResponse = httpClient.send(searchRequest, HttpResponse.BodyHandlers.ofString());
				if (getGroupResponse.statusCode() == 200) {
					JsonNode groups = objectMapper.readTree(getGroupResponse.body());
					if (groups.isArray() && groups.size() > 0) {
						for (JsonNode group : groups) {
							if (groupName.equals(group.get("name").asText())) {
								return group.get("id").asText();
							}
						}
					}
				}
				throw new KeycloakException("Group already exists but could not retrieve ID");
			} else {
				String errorMessage = String.format("Failed to create group in Keycloak. Status: %d, Response: %s",
					createResponse.statusCode(), createResponse.body());
				logger.error(errorMessage);
				throw new KeycloakException(errorMessage);
			}
		} catch (Exception e) {
			logger.error("Error creating group in Keycloak: name={}, error={}", groupName, e.getMessage(), e);
			throw new KeycloakException("Failed to create group in Keycloak: " + e.getMessage(), e);
		}
	}

	/**
	 * Assigns a user to a group in Keycloak (alternative to Organizations).
	 */
	public void assignUserToGroup(String groupId, String userId) {
		logger.info("Assigning user to group: groupId={}, userId={}", groupId, userId);

		try {
			String accessToken = getAdminAccessToken();
			
			// Verify user exists
			boolean userExists = verifyUserExists(userId, accessToken);
			if (!userExists) {
				throw new KeycloakException("Cannot assign user to group: User does not exist with ID: " + userId);
			}
			
			// Groups API is more reliable - no need for extensive retries
			String memberUrl = String.format("%s/admin/realms/%s/users/%s/groups/%s", 
				serverUrl, realm, userId, groupId);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(memberUrl))
				.header("Authorization", "Bearer " + accessToken)
				.PUT(HttpRequest.BodyPublishers.noBody())
				.timeout(Duration.ofSeconds(30))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			
			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				logger.info("Successfully assigned user to group: groupId={}, userId={}", groupId, userId);
			} else {
				String errorMessage = String.format("Failed to assign user to group. Status: %d, Response: %s",
					response.statusCode(), response.body());
				logger.error(errorMessage);
				throw new KeycloakException(errorMessage);
			}
		} catch (Exception e) {
			logger.error("Error assigning user to group: groupId={}, userId={}", groupId, userId, e);
			throw new KeycloakException("Failed to assign user to group: " + e.getMessage(), e);
		}
	}

	/**
	 * Deletes a group from Keycloak (alternative to Organizations).
	 */
	public void deleteGroup(String groupId) {
		logger.info("Deleting group from Keycloak: groupId={}", groupId);

		try {
			String accessToken = getAdminAccessToken();
			String groupUrl = String.format("%s/admin/realms/%s/groups/%s", serverUrl, realm, groupId);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(groupUrl))
				.header("Authorization", "Bearer " + accessToken)
				.DELETE()
				.timeout(Duration.ofSeconds(30))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				logger.info("Successfully deleted group from Keycloak: groupId={}", groupId);
			} else {
				String errorMessage = String.format("Failed to delete group from Keycloak. Status: %d, Response: %s",
					response.statusCode(), response.body());
				logger.error(errorMessage);
				throw new KeycloakException(errorMessage);
			}
		} catch (Exception e) {
			logger.error("Error deleting group from Keycloak: groupId={}", groupId, e);
			throw new KeycloakException("Failed to delete group from Keycloak: " + e.getMessage(), e);
		}
	}

	/**
	 * Internal method to verify that a user exists in Keycloak.
	 */
	private boolean verifyUserExists(String userId, String accessToken) {
		try {
			String userUrl = String.format("%s/admin/realms/%s/users/%s", serverUrl, realm, userId);
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(userUrl))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.timeout(Duration.ofSeconds(10))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			return response.statusCode() == 200;
		} catch (Exception e) {
			logger.warn("Failed to verify user exists: userId={}, error={}", userId, e.getMessage());
			return false;
		}
	}

	/**
	 * Assigns a user to an organization via REST API.
	 * 
	 * @param organizationId Organization ID
	 * @param userId User ID
	 * @throws KeycloakException if assignment fails
	 */
	public void assignUserToOrganization(String organizationId, String userId) {
		logger.info("Assigning user to organization: orgId={}, userId={}", organizationId, userId);

		try {
			String accessToken = getAdminAccessToken();
			
			// Step 1: Comprehensive user verification with multiple approaches
			boolean userExists = verifyUserExistsComprehensively(userId, accessToken);
			if (!userExists) {
				throw new KeycloakException("Cannot assign user to organization: User does not exist with ID: " + userId);
			}
			
			// Step 2: Additional wait for Keycloak to fully index the user for organization operations
			// Reduced since calling service now handles initial wait and verification
			logger.debug("Waiting 2 seconds for Keycloak to fully index user for organization operations...");
			Thread.sleep(2000);
			
			String membersUrl = String.format("%s/admin/realms/%s/organizations/%s/members", 
				serverUrl, realm, organizationId);

			// Step 3: Prepare payload - ensure clean JSON without extra whitespace
			// Based on GitHub issue #38760, payload formatting is critical
			JsonNode memberNode = objectMapper.createObjectNode()
				.put("id", userId.trim()); // Ensure no whitespace
			String memberJson = objectMapper.writeValueAsString(memberNode);
			
			// Log the exact payload being sent for debugging
			logger.debug("Sending organization assignment payload: {}", memberJson);

			// Step 4: Retry assignment with progressive delays and comprehensive error handling
			Exception lastException = null;
			int maxRetries = 15; // Increased retries
			long baseDelayMs = 2000; // Start with 2 seconds
			
			for (int attempt = 0; attempt < maxRetries; attempt++) {
				try {
					// Get fresh access token for each attempt to avoid token expiry
					if (attempt > 0) {
						accessToken = getAdminAccessToken();
					}
					
					HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(membersUrl))
						.header("Authorization", "Bearer " + accessToken)
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString(memberJson))
						.timeout(Duration.ofSeconds(45)) // Increased timeout
						.build();

					HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
					
					logger.debug("Assignment attempt {} response: Status={}, Body={}", 
						attempt + 1, response.statusCode(), response.body());

					if (response.statusCode() >= 200 && response.statusCode() < 300) {
						logger.info("Successfully assigned user to organization: orgId={}, userId={} (attempt {})", 
							organizationId, userId, attempt + 1);
						return; // Success, exit method
					} else {
						String responseBody = response.body();
						
						// Parse error response to determine if it's retryable
						boolean isRetryableError = isRetryableAssignmentError(response.statusCode(), responseBody);
						
						if (isRetryableError && attempt < maxRetries - 1) {
							// Re-verify user exists before retry
							boolean userStillExists = verifyUserExistsComprehensively(userId, accessToken);
							if (!userStillExists) {
								throw new KeycloakException(String.format(
									"Cannot assign user to organization: User no longer exists with ID: %s", userId));
							}
							
							// Progressive delay: 2s, 4s, 6s, 8s, 10s, then cap at 10s
							long delayMs = Math.min(baseDelayMs * (attempt + 1), 10000);
							logger.warn("Assignment failed (attempt {}/{}). Status: {}, Response: {}. " +
								"User verified exists. Retrying in {}ms...", 
								attempt + 1, maxRetries, response.statusCode(), responseBody, delayMs);
							Thread.sleep(delayMs);
							lastException = new KeycloakException(String.format(
								"Failed to assign user to organization. Status: %d, Response: %s",
								response.statusCode(), responseBody));
							continue; // Retry
						} else {
							// Non-retryable error or max retries reached
							String errorMessage = String.format("Failed to assign user to organization after %d attempts. " +
								"Status: %d, Response: %s", attempt + 1, response.statusCode(), responseBody);
							logger.error(errorMessage);
							throw new KeycloakException(errorMessage);
						}
					}
				} catch (KeycloakException e) {
					// Re-throw KeycloakException immediately (non-retryable)
					throw e;
				} catch (Exception e) {
					// For other exceptions, retry if not last attempt
					if (attempt < maxRetries - 1) {
						long delayMs = Math.min(baseDelayMs * (attempt + 1), 10000);
						logger.warn("Exception during assignment (attempt {}/{}). Retrying in {}ms: {}", 
							attempt + 1, maxRetries, delayMs, e.getMessage());
						try {
							Thread.sleep(delayMs);
						} catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
							throw new KeycloakException("Interrupted while retrying assignment", ie);
						}
						lastException = e;
						continue;
					} else {
						throw e; // Last attempt failed
					}
				}
			}
			
			// If we get here, all retries failed
			if (lastException != null) {
				throw new KeycloakException("Failed to assign user to organization after " + maxRetries + " attempts", lastException);
			}
			
		} catch (KeycloakException e) {
			throw e;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new KeycloakException("Interrupted while waiting for user to be available", e);
		} catch (Exception e) {
			logger.error("Error assigning user to organization: orgId={}, userId={}", organizationId, userId, e);
			throw new KeycloakException("Failed to assign user to organization: " + e.getMessage(), e);
		}
	}

	/**
	 * Comprehensive user verification using multiple approaches.
	 */
	private boolean verifyUserExistsComprehensively(String userId, String accessToken) throws Exception {
		// Approach 1: Direct user lookup by ID
		if (verifyUserExists(userId, accessToken)) {
			logger.debug("User verified via direct ID lookup: {}", userId);
			return true;
		}
		
		// Approach 2: Search by user ID with retry
		for (int i = 0; i < 5; i++) {
			String searchUrl = String.format("%s/admin/realms/%s/users/%s", serverUrl, realm, userId);
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(searchUrl))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.timeout(Duration.ofSeconds(10))
				.build();
			
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				logger.debug("User verified via search (attempt {}): {}", i + 1, userId);
				return true;
			}
			
			if (i < 4) {
				logger.debug("User search failed (attempt {}), retrying in 2s...", i + 1);
				Thread.sleep(2000);
			}
		}
		
		logger.warn("User verification failed after all attempts: {}", userId);
		return false;
	}

	/**
	 * Determines if an assignment error is retryable based on status code and response.
	 */
	private boolean isRetryableAssignmentError(int statusCode, String responseBody) {
		// 400 Bad Request is often retryable for organization assignments
		if (statusCode == 400) {
			return true;
		}
		
		// Check for specific error messages that indicate timing issues
		if (responseBody != null) {
			try {
				JsonNode errorJson = objectMapper.readTree(responseBody);
				if (errorJson.has("errorMessage")) {
					String errorMsg = errorJson.get("errorMessage").asText().toLowerCase();
					return errorMsg.contains("user does not exist") || 
						   errorMsg.contains("user not found") ||
						   errorMsg.contains("not found") ||
						   errorMsg.contains("invalid user");
				}
			} catch (Exception e) {
				// Not JSON, check as plain text
				String lowerBody = responseBody.toLowerCase();
				return lowerBody.contains("user does not exist") || 
					   lowerBody.contains("user not found") ||
					   lowerBody.contains("not found") ||
					   lowerBody.contains("invalid user");
			}
		}
		
		// 500 errors might be temporary
		return statusCode >= 500 && statusCode < 600;
	}

	/**
	 * Assigns a role to a user within an organization via REST API.
	 * 
	 * @param organizationId Organization ID
	 * @param userId User ID
	 * @param roleName Role name to assign
	 * @throws KeycloakException if role assignment fails
	 */
	public void assignRoleToUser(String organizationId, String userId, String roleName) {
		logger.info("Assigning role to user in organization: orgId={}, userId={}, role={}", 
			organizationId, userId, roleName);

		try {
			String accessToken = getAdminAccessToken();
			
			// First, get organization roles
			String rolesUrl = String.format("%s/admin/realms/%s/organizations/%s/roles", 
				serverUrl, realm, organizationId);
			
			HttpRequest rolesRequest = HttpRequest.newBuilder()
				.uri(URI.create(rolesUrl))
				.header("Authorization", "Bearer " + accessToken)
				.GET()
				.timeout(Duration.ofSeconds(10))
				.build();

			HttpResponse<String> rolesResponse = httpClient.send(rolesRequest, HttpResponse.BodyHandlers.ofString());
			
			if (rolesResponse.statusCode() != 200) {
				throw new KeycloakException("Failed to get organization roles: " + rolesResponse.body());
			}

			// Find the role
			JsonNode rolesJson = objectMapper.readTree(rolesResponse.body());
			JsonNode role = null;
			if (rolesJson.isArray()) {
				for (JsonNode r : rolesJson) {
					if (roleName.equals(r.get("name").asText())) {
						role = r;
						break;
					}
				}
			}

			if (role == null) {
				throw new KeycloakException("Role '" + roleName + "' not found in organization");
			}

			// Assign role to user
			String assignUrl = String.format("%s/admin/realms/%s/organizations/%s/members/%s/roles", 
				serverUrl, realm, organizationId, userId);
			
			String roleJson = "[" + objectMapper.writeValueAsString(role) + "]";
			
			HttpRequest assignRequest = HttpRequest.newBuilder()
				.uri(URI.create(assignUrl))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(roleJson))
				.timeout(Duration.ofSeconds(30))
				.build();

			HttpResponse<String> assignResponse = httpClient.send(assignRequest, HttpResponse.BodyHandlers.ofString());

			if (assignResponse.statusCode() >= 200 && assignResponse.statusCode() < 300) {
				logger.info("Successfully assigned role to user: orgId={}, userId={}, role={}", 
					organizationId, userId, roleName);
			} else {
				String errorMessage = String.format("Failed to assign role to user. Status: %d, Response: %s",
					assignResponse.statusCode(), assignResponse.body());
				logger.error(errorMessage);
				throw new KeycloakException(errorMessage);
			}
		} catch (KeycloakException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Error assigning role to user: orgId={}, userId={}, role={}", 
				organizationId, userId, roleName, e);
			throw new KeycloakException("Failed to assign role to user: " + e.getMessage(), e);
		}
	}

	/**
	 * Deletes an organization from Keycloak.
	 * Used for rollback/compensation.
	 * 
	 * @param organizationId Organization ID
	 */
	public void deleteOrganization(String organizationId) {
		logger.info("Deleting organization from Keycloak: orgId={}", organizationId);

		try {
			String accessToken = getAdminAccessToken();
			String orgUrl = String.format("%s/admin/realms/%s/organizations/%s", 
				serverUrl, realm, organizationId);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(orgUrl))
				.header("Authorization", "Bearer " + accessToken)
				.DELETE()
				.timeout(Duration.ofSeconds(30))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				logger.info("Successfully deleted organization from Keycloak: orgId={}", organizationId);
			} else if (response.statusCode() == 404) {
				logger.warn("Organization not found for deletion: orgId={}", organizationId);
			} else {
				logger.error("Failed to delete organization: orgId={}, status={}, response={}", 
					organizationId, response.statusCode(), response.body());
			}
		} catch (Exception e) {
			logger.error("Error deleting organization from Keycloak: orgId={}", organizationId, e);
			// Log but don't throw - rollback should continue even if cleanup fails
		}
	}

	/**
	 * Deletes a user from Keycloak.
	 * Used for rollback/compensation.
	 * 
	 * @param userId User ID
	 */
	public void deleteUser(String userId) {
		logger.info("Deleting user from Keycloak: userId={}", userId);

		try (Keycloak keycloak = KeycloakBuilder.builder()
				.serverUrl(serverUrl)
				.realm("master")  // Authenticate against master realm for admin privileges
				.clientId("admin-cli")
				.username(username)
				.password(password)
				.grantType("password")
				.build()) {
			
			RealmResource realmResource = keycloak.realm(realm);  // But delete user from kymatic realm
			UsersResource usersResource = realmResource.users();
			UserResource userResource = usersResource.get(userId);
			userResource.remove();

			logger.info("Successfully deleted user from Keycloak: userId={}", userId);
		} catch (jakarta.ws.rs.NotFoundException e) {
			logger.warn("User not found for deletion: userId={}", userId);
			// User already deleted or doesn't exist - this is fine for rollback
		} catch (Exception e) {
			logger.error("Error deleting user from Keycloak: userId={}", userId, e);
			// Log but don't throw - rollback should continue even if cleanup fails
		}
	}

}

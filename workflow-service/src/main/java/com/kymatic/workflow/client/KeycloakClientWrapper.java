package com.kymatic.workflow.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Wrapper for Keycloak Admin Client and REST API to manage Organizations and Users.
 * Used by workflow-service to orchestrate tenant onboarding.
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

	private Keycloak getKeycloakAdminClient() {
		KeycloakBuilder builder = KeycloakBuilder.builder()
			.serverUrl(serverUrl)
			.realm(realm)
			.clientId(clientId)
			.username(username)
			.password(password);

		if (clientSecret != null && !clientSecret.isBlank()) {
			builder.clientSecret(clientSecret);
		}

		return builder.build();
	}

	private String getAdminAccessToken() {
		try {
			Keycloak keycloak = getKeycloakAdminClient();
			return keycloak.tokenManager().getAccessTokenString();
		} catch (Exception e) {
			logger.error("Failed to get admin access token", e);
			throw new RuntimeException("Failed to authenticate with Keycloak: " + e.getMessage(), e);
		}
	}

	/**
	 * Creates a new organization in Keycloak via REST API.
	 */
	public String createOrganization(String alias, String name) {
		logger.info("Creating organization in Keycloak: alias={}, name={}", alias, name);

		if (organizationExists(alias)) {
			throw new RuntimeException("Organization with alias '" + alias + "' already exists in Keycloak");
		}

		try {
			String accessToken = getAdminAccessToken();
			String orgsUrl = String.format("%s/admin/realms/%s/organizations", serverUrl, realm);

			String orgJson = String.format(
				"{\"alias\":\"%s\",\"name\":\"%s\",\"enabled\":true}",
				alias, name.replace("\"", "\\\"")
			);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(orgsUrl))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(orgJson))
				.timeout(Duration.ofSeconds(30))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 201) {
				String location = response.headers().firstValue("Location").orElse("");
				String orgId = location.isEmpty() ? extractOrgIdFromResponse(response.body()) : 
					location.substring(location.lastIndexOf('/') + 1);
				
				logger.info("Successfully created organization in Keycloak: alias={}, id={}", alias, orgId);
				return orgId;
			} else if (response.statusCode() == 409) {
				throw new RuntimeException("Organization with alias '" + alias + "' already exists in Keycloak");
			} else {
				throw new RuntimeException("Failed to create organization in Keycloak. Status: " + 
					response.statusCode() + ", Response: " + response.body());
			}
		} catch (Exception e) {
			logger.error("Error creating organization in Keycloak: alias={}", alias, e);
			throw new RuntimeException("Failed to create organization in Keycloak: " + e.getMessage(), e);
		}
	}

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

	public static class OrganizationInfo {
		private final String id;
		private final String alias;
		private final String name;

		public OrganizationInfo(String id, String alias, String name) {
			this.id = id;
			this.alias = alias;
			this.name = name;
		}

		public String getId() { return id; }
		public String getAlias() { return alias; }
		public String getName() { return name; }
	}

	/**
	 * Creates a new user in Keycloak.
	 */
	public String createUser(String email, String password, String firstName, String lastName, boolean emailVerified) {
		logger.info("Creating user in Keycloak: email={}", email);

		try (Keycloak keycloak = getKeycloakAdminClient()) {
			RealmResource realmResource = keycloak.realm(realm);
			UsersResource usersResource = realmResource.users();

			List<UserRepresentation> existingUsers = usersResource.searchByEmail(email, true);
			if (!existingUsers.isEmpty()) {
				throw new RuntimeException("User with email '" + email + "' already exists in Keycloak");
			}

			UserRepresentation user = new UserRepresentation();
			user.setUsername(email);
			user.setEmail(email);
			user.setFirstName(firstName);
			user.setLastName(lastName);
			user.setEmailVerified(emailVerified);
			user.setEnabled(true);

			jakarta.ws.rs.core.Response response = usersResource.create(user);

			if (response.getStatus() == jakarta.ws.rs.core.Response.Status.CREATED.getStatusCode()) {
				String location = response.getLocation().toString();
				String userId = location.substring(location.lastIndexOf('/') + 1);

				CredentialRepresentation credential = new CredentialRepresentation();
				credential.setType(CredentialRepresentation.PASSWORD);
				credential.setValue(password);
				credential.setTemporary(false);

				UserResource userResource = usersResource.get(userId);
				userResource.resetPassword(credential);

				logger.info("Successfully created user in Keycloak: email={}, id={}", email, userId);
				return userId;
			} else if (response.getStatus() == jakarta.ws.rs.core.Response.Status.CONFLICT.getStatusCode()) {
				throw new RuntimeException("User with email '" + email + "' already exists in Keycloak");
			} else {
				throw new RuntimeException("Failed to create user in Keycloak. Status: " + 
					response.getStatus() + ", Response: " + response.readEntity(String.class));
			}
		} catch (Exception e) {
			logger.error("Error creating user in Keycloak: email={}", email, e);
			throw new RuntimeException("Failed to create user in Keycloak: " + e.getMessage(), e);
		}
	}

	/**
	 * Assigns a user to an organization via REST API.
	 */
	public void assignUserToOrganization(String organizationId, String userId) {
		logger.info("Assigning user to organization: orgId={}, userId={}", organizationId, userId);

		try {
			String accessToken = getAdminAccessToken();
			String membersUrl = String.format("%s/admin/realms/%s/organizations/%s/members", 
				serverUrl, realm, organizationId);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(membersUrl))
				.header("Authorization", "Bearer " + accessToken)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("[\"" + userId + "\"]"))
				.timeout(Duration.ofSeconds(30))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				logger.info("Successfully assigned user to organization: orgId={}, userId={}", organizationId, userId);
			} else {
				throw new RuntimeException("Failed to assign user to organization. Status: " + 
					response.statusCode() + ", Response: " + response.body());
			}
		} catch (Exception e) {
			logger.error("Error assigning user to organization: orgId={}, userId={}", organizationId, userId, e);
			throw new RuntimeException("Failed to assign user to organization: " + e.getMessage(), e);
		}
	}

	/**
	 * Assigns a role to a user within an organization via REST API.
	 */
	public void assignRoleToUser(String organizationId, String userId, String roleName) {
		logger.info("Assigning role to user in organization: orgId={}, userId={}, role={}", 
			organizationId, userId, roleName);

		try {
			String accessToken = getAdminAccessToken();
			
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
				throw new RuntimeException("Failed to get organization roles: " + rolesResponse.body());
			}

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
				logger.warn("Role '{}' not found in organization, skipping role assignment", roleName);
				return; // Don't fail if role doesn't exist
			}

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
				logger.warn("Failed to assign role to user (non-critical): Status: {}, Response: {}",
					assignResponse.statusCode(), assignResponse.body());
			}
		} catch (Exception e) {
			logger.warn("Error assigning role to user (non-critical): orgId={}, userId={}, role={}", 
				organizationId, userId, roleName, e);
			// Don't throw - role assignment is optional
		}
	}

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

			httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			logger.info("Successfully deleted organization from Keycloak: orgId={}", organizationId);
		} catch (Exception e) {
			logger.error("Error deleting organization from Keycloak: orgId={}", organizationId, e);
		}
	}

	public void deleteUser(String userId) {
		logger.info("Deleting user from Keycloak: userId={}", userId);
		try (Keycloak keycloak = getKeycloakAdminClient()) {
			RealmResource realmResource = keycloak.realm(realm);
			UsersResource usersResource = realmResource.users();
			UserResource userResource = usersResource.get(userId);
			userResource.remove();
			logger.info("Successfully deleted user from Keycloak: userId={}", userId);
		} catch (Exception e) {
			logger.error("Error deleting user from Keycloak: userId={}", userId, e);
		}
	}
}


package com.kymatic.workflow.delegate;

import com.kymatic.workflow.client.KeycloakClientWrapper;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Camunda delegate for assigning user to organization and assigning roles.
 * 
 * Expected workflow variables:
 * - keycloakOrganizationId: Organization ID
 * - adminUserId: User ID
 * - adminRole: Role to assign (optional, defaults to "admin")
 * 
 * Sets workflow variables:
 * - userAssigned: true
 */
@Component("userAssignmentDelegate")
public class UserAssignmentDelegate implements JavaDelegate {

	private static final Logger logger = LoggerFactory.getLogger(UserAssignmentDelegate.class);

	private final KeycloakClientWrapper keycloakClientWrapper;

	public UserAssignmentDelegate(KeycloakClientWrapper keycloakClientWrapper) {
		this.keycloakClientWrapper = keycloakClientWrapper;
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		String organizationId = (String) execution.getVariable("keycloakOrganizationId");
		String userId = (String) execution.getVariable("adminUserId");
		String role = (String) execution.getVariable("adminRole");
		
		if (role == null || role.isBlank()) {
			role = "admin"; // Default role
		}

		if (organizationId == null || organizationId.isBlank()) {
			throw new IllegalArgumentException("Organization ID is required for user assignment");
		}
		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("User ID is required for user assignment");
		}

		logger.info("Assigning user to organization: orgId={}, userId={}, role={}", 
			organizationId, userId, role);

		// Assign user to organization
		keycloakClientWrapper.assignUserToOrganization(organizationId, userId);

		// Assign role to user (non-blocking - won't fail if role doesn't exist)
		try {
			keycloakClientWrapper.assignRoleToUser(organizationId, userId, role);
		} catch (Exception e) {
			logger.warn("Failed to assign role '{}' to user (non-critical): {}", role, e.getMessage());
			// Continue - role assignment is optional
		}

		execution.setVariable("userAssigned", true);

		logger.info("User assigned to organization successfully: orgId={}, userId={}, role={}", 
			organizationId, userId, role);
	}
}


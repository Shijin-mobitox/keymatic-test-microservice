package com.kymatic.workflow.delegate;

import com.kymatic.workflow.client.KeycloakClientWrapper;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Camunda delegate for creating user in Keycloak.
 * 
 * Expected workflow variables:
 * - adminEmail: User email
 * - adminPassword: User password
 * - adminFirstName: User first name
 * - adminLastName: User last name
 * - adminEmailVerified: Whether email is verified (optional, defaults to false)
 * 
 * Sets workflow variables:
 * - adminUserId: Created user ID
 * - userCreated: true
 */
@Component("userCreationDelegate")
public class UserCreationDelegate implements JavaDelegate {

	private static final Logger logger = LoggerFactory.getLogger(UserCreationDelegate.class);

	private final KeycloakClientWrapper keycloakClientWrapper;

	public UserCreationDelegate(KeycloakClientWrapper keycloakClientWrapper) {
		this.keycloakClientWrapper = keycloakClientWrapper;
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		String email = (String) execution.getVariable("adminEmail");
		String password = (String) execution.getVariable("adminPassword");
		String firstName = (String) execution.getVariable("adminFirstName");
		String lastName = (String) execution.getVariable("adminLastName");
		Boolean emailVerified = execution.getVariable("adminEmailVerified") != null ? 
			(Boolean) execution.getVariable("adminEmailVerified") : false;

		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Admin email is required for user creation");
		}
		if (password == null || password.isBlank()) {
			throw new IllegalArgumentException("Admin password is required for user creation");
		}
		if (firstName == null || firstName.isBlank()) {
			throw new IllegalArgumentException("Admin first name is required for user creation");
		}
		if (lastName == null || lastName.isBlank()) {
			throw new IllegalArgumentException("Admin last name is required for user creation");
		}

		logger.info("Creating admin user in Keycloak: email={}", email);

		String userId = keycloakClientWrapper.createUser(email, password, firstName, lastName, emailVerified);

		execution.setVariable("adminUserId", userId);
		execution.setVariable("userCreated", true);

		logger.info("User created successfully in Keycloak: id={}, email={}", userId, email);
	}
}


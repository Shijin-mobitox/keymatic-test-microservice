package com.kymatic.workflow.delegate;

import com.kymatic.workflow.client.KeycloakClientWrapper;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Camunda delegate for creating organization in Keycloak.
 * 
 * Expected workflow variables:
 * - slug: Organization alias
 * - tenantName: Organization display name
 * 
 * Sets workflow variables:
 * - keycloakOrganizationId: Created organization ID
 */
@Component("organizationCreationDelegate")
public class OrganizationCreationDelegate implements JavaDelegate {

	private static final Logger logger = LoggerFactory.getLogger(OrganizationCreationDelegate.class);

	private final KeycloakClientWrapper keycloakClientWrapper;

	public OrganizationCreationDelegate(KeycloakClientWrapper keycloakClientWrapper) {
		this.keycloakClientWrapper = keycloakClientWrapper;
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		String slug = (String) execution.getVariable("slug");
		String tenantName = (String) execution.getVariable("tenantName");

		if (slug == null || slug.isBlank()) {
			throw new IllegalArgumentException("Slug is required for organization creation");
		}
		if (tenantName == null || tenantName.isBlank()) {
			throw new IllegalArgumentException("Tenant name is required for organization creation");
		}

		logger.info("Creating organization in Keycloak: alias={}, name={}", slug, tenantName);

		String organizationId = keycloakClientWrapper.createOrganization(slug, tenantName);

		execution.setVariable("keycloakOrganizationId", organizationId);
		execution.setVariable("organizationCreated", true);

		logger.info("Organization created successfully: id={}, alias={}", organizationId, slug);
	}
}


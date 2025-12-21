package com.kymatic.tenantservice.exception;

/**
 * Exception thrown when attempting to create an organization that already exists.
 */
public class OrganizationAlreadyExistsException extends RuntimeException {
    
    public OrganizationAlreadyExistsException(String alias) {
        super("Organization with alias '" + alias + "' already exists in Keycloak");
    }
}


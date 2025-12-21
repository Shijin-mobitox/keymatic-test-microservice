package com.kymatic.tenantservice.exception;

/**
 * Exception thrown when attempting to create a user that already exists.
 */
public class UserAlreadyExistsException extends RuntimeException {
    
    public UserAlreadyExistsException(String email) {
        super("User with email '" + email + "' already exists in Keycloak");
    }
}


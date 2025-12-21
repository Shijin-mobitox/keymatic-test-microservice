package com.kymatic.tenantservice.exception;

/**
 * Exception thrown when Keycloak operations fail.
 */
public class KeycloakException extends RuntimeException {
    
    public KeycloakException(String message) {
        super(message);
    }
    
    public KeycloakException(String message, Throwable cause) {
        super(message, cause);
    }
}


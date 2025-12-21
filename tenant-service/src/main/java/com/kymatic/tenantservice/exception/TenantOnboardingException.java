package com.kymatic.tenantservice.exception;

/**
 * Exception thrown when tenant onboarding fails.
 * This is used for rollback/compensation scenarios.
 */
public class TenantOnboardingException extends RuntimeException {
    
    private final String tenantAlias;
    private final OnboardingStep failedStep;
    
    public TenantOnboardingException(String tenantAlias, OnboardingStep failedStep, String message) {
        super(String.format("Tenant onboarding failed at step '%s' for tenant '%s': %s", 
            failedStep, tenantAlias, message));
        this.tenantAlias = tenantAlias;
        this.failedStep = failedStep;
    }
    
    public TenantOnboardingException(String tenantAlias, OnboardingStep failedStep, String message, Throwable cause) {
        super(String.format("Tenant onboarding failed at step '%s' for tenant '%s': %s", 
            failedStep, tenantAlias, message), cause);
        this.tenantAlias = tenantAlias;
        this.failedStep = failedStep;
    }
    
    public String getTenantAlias() {
        return tenantAlias;
    }
    
    public OnboardingStep getFailedStep() {
        return failedStep;
    }
    
    public enum OnboardingStep {
        KEYCLOAK_ORG_CREATION,
        DATABASE_CREATION,
        DATABASE_MIGRATION,
        USER_CREATION,
        USER_ORG_ASSIGNMENT,
        ROLE_ASSIGNMENT
    }
}


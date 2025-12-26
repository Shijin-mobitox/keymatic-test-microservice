package com.kymatic.workflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserCreationRequest(
    @NotBlank String tenantId,
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotBlank String firstName,
    @NotBlank String lastName,
    Boolean emailVerified,
    String role
) {
}

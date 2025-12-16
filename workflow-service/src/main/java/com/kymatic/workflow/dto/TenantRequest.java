package com.kymatic.workflow.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TenantRequest(
    @NotBlank
    @Size(max = 255)
    String tenantName,

    @NotBlank
    @Pattern(regexp = "^[a-z0-9\\-]+$", message = "Slug must contain lowercase letters, digits or hyphens")
    @Size(max = 100)
    String slug,

    @NotBlank
    String subscriptionTier,

    @NotNull
    Integer maxUsers,

    @NotNull
    Integer maxStorageGb,

    @Email
    String adminEmail,

    JsonNode metadata
) {}


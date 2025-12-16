package com.kymatic.tenantservice.dto.rbac;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSiteRequest(
    @NotBlank @Size(max = 255) String siteName,
    @NotBlank @Size(max = 50) String siteCode,
    String address,
    String city,
    String state,
    String country,
    String postalCode,
    String phone,
    String email,
    Boolean headquarters
) {}


package com.kymatic.tenantservice.dto;

/**
 * Login response DTO
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        String userId,
        String email,
        String tenantId,
        String role
) {
}


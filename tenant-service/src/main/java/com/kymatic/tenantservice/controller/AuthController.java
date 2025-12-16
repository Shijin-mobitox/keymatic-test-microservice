package com.kymatic.tenantservice.controller;

import com.kymatic.tenantservice.dto.LoginRequest;
import com.kymatic.tenantservice.dto.LoginResponse;
import com.kymatic.tenantservice.persistence.entity.TenantUserEntity;
import com.kymatic.tenantservice.persistence.repository.TenantUserRepository;
import com.kymatic.tenantservice.service.TenantIdentifierResolver;
import com.kymatic.tenantservice.util.JwtTokenUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Local authentication controller for login without Keycloak
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Allow CORS for local development
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final TenantUserRepository tenantUserRepository;
    private final TenantIdentifierResolver tenantIdentifierResolver;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthController(
            TenantUserRepository tenantUserRepository,
            TenantIdentifierResolver tenantIdentifierResolver,
            PasswordEncoder passwordEncoder,
            JwtTokenUtil jwtTokenUtil) {
        this.tenantUserRepository = tenantUserRepository;
        this.tenantIdentifierResolver = tenantIdentifierResolver;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        try {
            // Resolve tenant ID from header or request
            String tenantIdentifier = request.tenantId() != null ? request.tenantId() : tenantHeader;
            if (tenantIdentifier == null || tenantIdentifier.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Tenant ID is required"));
            }

            UUID tenantId = tenantIdentifierResolver.resolveTenantIdentifier(tenantIdentifier);

            // Find user by email and tenant
            Optional<TenantUserEntity> userOpt = tenantUserRepository.findByTenantIdAndEmail(tenantId, request.email());
            if (userOpt.isEmpty()) {
                logger.warn("Login attempt failed: User not found for email {} in tenant {}", request.email(), tenantIdentifier);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials"));
            }

            TenantUserEntity user = userOpt.get();

            // Check if user is active
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                logger.warn("Login attempt failed: User {} is inactive", request.email());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Account is inactive"));
            }

            // Verify password
            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                logger.warn("Login attempt failed: Invalid password for user {}", request.email());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials"));
            }

            // Update last login
            user.setLastLogin(OffsetDateTime.now());
            tenantUserRepository.save(user);

            // Generate JWT token
            String accessToken = jwtTokenUtil.generateToken(
                    user.getUserId().toString(),
                    user.getEmail(),
                    tenantId.toString(),
                    user.getRole()
            );

            String refreshToken = jwtTokenUtil.generateRefreshToken(user.getUserId().toString());

            logger.info("User {} successfully logged in for tenant {}", user.getEmail(), tenantIdentifier);

            // Build response
            LoginResponse response = new LoginResponse(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    jwtTokenUtil.getTokenExpirationSeconds(),
                    user.getUserId().toString(),
                    user.getEmail(),
                    tenantId.toString(),
                    user.getRole()
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Login error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refresh_token");
            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Refresh token is required"));
            }

            // Validate and extract user ID from refresh token
            String userId = jwtTokenUtil.validateRefreshToken(refreshToken);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid refresh token"));
            }

            // Get user from database
            UUID userUuid = UUID.fromString(userId);
            TenantUserEntity user = tenantUserRepository.findById(userUuid)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (!Boolean.TRUE.equals(user.getIsActive())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Account is inactive"));
            }

            // Generate new access token
            String newAccessToken = jwtTokenUtil.generateToken(
                    user.getUserId().toString(),
                    user.getEmail(),
                    user.getTenantId().toString(),
                    user.getRole()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("access_token", newAccessToken);
            response.put("token_type", "Bearer");
            response.put("expires_in", jwtTokenUtil.getTokenExpirationSeconds());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error refreshing token", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid refresh token"));
        }
    }
}


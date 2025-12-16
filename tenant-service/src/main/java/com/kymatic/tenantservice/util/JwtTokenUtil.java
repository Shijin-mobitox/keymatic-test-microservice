package com.kymatic.tenantservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for generating and validating JWT tokens for local authentication
 */
@Component
public class JwtTokenUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${app.jwt.secret:your-256-bit-secret-key-change-this-in-production-min-32-chars}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:3600}")
    private Long jwtExpiration; // seconds

    @Value("${app.jwt.refresh-expiration:86400}")
    private Long refreshExpiration; // 24 hours in seconds

    @Value("${app.jwt.issuer:http://localhost:8083}")
    private String jwtIssuer;

    private static final String LOCAL_ISSUER_PREFIX = "http://localhost:8083";

    /**
     * Generate access token for authenticated user
     */
    public String generateToken(String userId, String email, String tenantId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        claims.put("preferred_username", email);
        claims.put("email", email);
        claims.put("tenant_id", tenantId);
        claims.put("role", role);

        return createToken(claims, userId, jwtExpiration);
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        claims.put("type", "refresh");

        return createToken(claims, userId, refreshExpiration);
    }

    /**
     * Validate refresh token and return user ID
     */
    public String validateRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            if (!"refresh".equals(claims.get("type"))) {
                logger.warn("Token is not a refresh token");
                return null;
            }
            return claims.getSubject();
        } catch (Exception e) {
            logger.error("Failed to validate refresh token", e);
            return null;
        }
    }

    /**
     * Parse and validate JWT token
     */
    public Claims parseToken(String token) {
        try {
            SecretKey key = getSigningKey();
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            logger.error("Failed to parse JWT token", e);
            throw new RuntimeException("Invalid token", e);
        }
    }

    /**
     * Get token expiration time in seconds
     */
    public Long getTokenExpirationSeconds() {
        return jwtExpiration;
    }

    /**
     * Create JWT token with specified claims and expiration
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Instant now = Instant.now();
        Instant expirationTime = now.plusSeconds(expiration);

        SecretKey key = getSigningKey();

        // For jjwt 0.12.x, we need to use the new fluent API
        // The subject should be in claims or set separately
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(jwtIssuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expirationTime))
                .signWith(key)
                .compact();
    }

    /**
     * Get signing key from secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        // Ensure key is at least 32 bytes (256 bits) for HS256
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            return Keys.hmacShaKeyFor(paddedKey);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}


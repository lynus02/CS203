package com.lynus.cs203.config;

import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "spring.jwt")
@Data
public class JwtConfig {
    private String secret;
    private int accessTokenExpiration; // in seconds
    private int refreshTokenExpiration; // in seconds

    public SecretKey getSecretKey() {
        log.debug("Creating SecretKey from JWT configuration");

        if (secret == null || secret.trim().isEmpty()) {
            log.error("JWT secret is null or empty - JWT operations will fail");
            throw new IllegalStateException("JWT secret is not configured");
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            log.debug("Successfully created SecretKey - Access token expiration: {}s, Refresh token expiration: {}s",
                    accessTokenExpiration, refreshTokenExpiration);
            return key;
        } catch (Exception e) {
            log.error("Failed to create SecretKey from JWT secret", e);
            throw new RuntimeException("JWT secret key creation failed", e);
        }
    }
}

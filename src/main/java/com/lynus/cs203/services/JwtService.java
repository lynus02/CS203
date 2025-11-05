package com.lynus.cs203.services;

import com.lynus.cs203.config.JwtConfig;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.repositories.UserRoleRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class JwtService {
    private final JwtConfig jwtConfig;
    private final UserRoleRepository userRoleRepository;

    public String generateAccessToken(User user) {
        log.info("Generating Access token for user: {}", user.getUserId());
        return generateToken(user, jwtConfig.getAccessTokenExpiration());
    }

    public String generateRefreshToken(User user) {
        log.info("Generating Refresh token for user: {}", user.getUserId());
        return generateToken(user, jwtConfig.getRefreshTokenExpiration());
    }

    private String generateToken(User user, long tokenExpiration) {
        String firstName = user.getUserProfile() != null ? user.getUserProfile().getFirstName() : null;
        String lastName = user.getUserProfile() != null ? user.getUserProfile().getLastName() : null;

        List<String> roles = getUserRoles(user.getUserId());

        log.debug("Building token claims for user: {} - Roles: {}", user.getUserId(), roles.size());
        log.trace("Token details - Email: {}, FirstName: {}, LastName: {}, Roles: {}",
                user.getEmail(), firstName, lastName, roles);

        Date expiration = new Date(System.currentTimeMillis() + (tokenExpiration * 1000));

        String token = Jwts.builder()
                .subject(user.getUserId())
                .claim("email", user.getEmail())
                .claim("firstName", firstName)
                .claim("lastName", lastName)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(jwtConfig.getSecretKey())
                .compact();

        log.info("Successfully generated JWT token for user: {} - Expires: {}", user.getUserId(), expiration);
        return token;
    }

    public boolean validateToken(String token) {
        log.debug("Validating JWT token");

        try {
            var claims = getClaims(token);
            boolean isValid = claims.getExpiration().after(new Date());
            log.debug("Token validation result: {}", isValid);
            return isValid; // Return the actual validation result
        } catch (JwtException e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            return false; // Return false for invalid tokens
        }
    }

    public String extractUserId(String token) {
        log.debug("Extracting user ID from JWT token");
        return getClaims(token).getSubject();
    }

    public List<String> extractRoles(String token) {
        log.debug("Extracting roles from JWT token");
        Object rolesObj = getClaims(token).get("roles");
        if (rolesObj instanceof List<?> rolesList) {
            return rolesList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

    public String extractEmail(String token) {
        log.debug("Extracting email from JWT token");
        return getClaims(token).get("email", String.class);
    }

    public String extractFirstName(String token) {
        log.debug("Extracting first name from JWT token");
        return getClaims(token).get("firstName", String.class);
    }

    public String extractLastName(String token) {
        log.debug("Extracting last name from JWT token");
        return getClaims(token).get("lastName", String.class);
    }

    private Claims getClaims(String token) {
        log.debug("Parsing JWT token claims");
        return Jwts.parser()
                .verifyWith(jwtConfig.getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private List<String> getUserRoles(String userId) {
        return userRoleRepository.findByUserUserId(userId)  // Change this line
                .stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();
    }
}

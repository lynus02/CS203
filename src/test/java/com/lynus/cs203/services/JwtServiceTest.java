package com.lynus.cs203.services;

import com.lynus.cs203.config.JwtConfig;
import com.lynus.cs203.entities.Role;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.entities.UserProfile;
import com.lynus.cs203.entities.UserRole;
import com.lynus.cs203.repositories.UserRoleRepository;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.checkerframework.checker.fenum.qual.SwingHorizontalOrientation;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Unit Test")
class JwtServiceTest {

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private JwtService jwtService;

    @Test
    @DisplayName("Should generate access token successfully for valid user")
    void generateAccessToken_WhenValidUser_ShouldReturnToken() {
        // Arrange
        User user = createTestUser();
        int tokenExpiration = 3600;
        SecretKey secretKey = createTestSecretKey();

        when(jwtConfig.getAccessTokenExpiration()).thenReturn(tokenExpiration);
        when(jwtConfig.getSecretKey()).thenReturn(secretKey);
        when(userRoleRepository.findByUserUserId("userId")).thenReturn(createTestUserRoles());

        // Act
        String result = jwtService.generateAccessToken(user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();

        // Verify
        verify(jwtConfig).getAccessTokenExpiration();
        verify(jwtConfig).getSecretKey();
        verify(userRoleRepository).findByUserUserId("userId");
    }

    @Test
    @DisplayName("Should handle null UserProfile when generating access token")
    void generateAccessToken_WhenUserHasNullUserProfile_ShouldHandleNullProfile() {
        // Arrange
        User user = createTestUser();
        user.setUserProfile(null); // Set UserProfile to null

        int tokenExpiration = 3600;
        SecretKey secretKey = createTestSecretKey();

        when(jwtConfig.getAccessTokenExpiration()).thenReturn(tokenExpiration);
        when(jwtConfig.getSecretKey()).thenReturn(secretKey);
        when(userRoleRepository.findByUserUserId("userId")).thenReturn(createTestUserRoles());

        // Act
        String result = jwtService.generateAccessToken(user);

        // Assert
        assertThat(result).isNotNull().isNotEmpty();

        // Verify the token can be parsed and has null values for firstName/lastName
        var parsedClaims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(result)
                .getPayload();

        assertThat(parsedClaims.get("firstName")).isNull();
        assertThat(parsedClaims.get("lastName")).isNull();
        assertThat(parsedClaims.getSubject()).isEqualTo("userId");
        assertThat(parsedClaims.get("email")).isEqualTo("test@example.com");

        verify(jwtConfig).getAccessTokenExpiration();
        verify(jwtConfig).getSecretKey();
        verify(userRoleRepository).findByUserUserId("userId");
    }

    @Test
    @DisplayName("Should generate refresh token successfully for valid user")
    void generateRefreshToken_WhenValidUser_ShouldReturnToken() {
        // Arrange
        User user = createTestUser();
        int tokenExpiration = 604800;
        SecretKey secretKey = createTestSecretKey();

        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(tokenExpiration);
        when(jwtConfig.getSecretKey()).thenReturn(secretKey);
        when(userRoleRepository.findByUserUserId("userId")).thenReturn(createTestUserRoles());

        // Act
        String result = jwtService.generateRefreshToken(user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();

        // Verify
        verify(jwtConfig).getRefreshTokenExpiration();
        verify(jwtConfig).getSecretKey();
        verify(userRoleRepository).findByUserUserId("userId");
    }

    @Test
    @DisplayName("Should handle null firstName and lastName when generating access token")
    void generateAccessToken_WhenUserProfileHasNullNames_ShouldHandleNullNames() {
        // Arrange
        User user = createTestUser();
        user.getUserProfile().setFirstName(null); // Set firstName to null
        user.getUserProfile().setLastName(null);  // Set lastName to null

        int tokenExpiration = 3600;
        SecretKey secretKey = createTestSecretKey();

        when(jwtConfig.getAccessTokenExpiration()).thenReturn(tokenExpiration);
        when(jwtConfig.getSecretKey()).thenReturn(secretKey);
        when(userRoleRepository.findByUserUserId("userId")).thenReturn(createTestUserRoles());

        // Act
        String result = jwtService.generateAccessToken(user);

        // Assert
        assertThat(result).isNotNull().isNotEmpty();

        // Verify the token can be parsed and has null values for firstName/lastName
        var parsedClaims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(result)
                .getPayload();

        assertThat(parsedClaims.get("firstName")).isNull();
        assertThat(parsedClaims.get("lastName")).isNull();
        assertThat(parsedClaims.getSubject()).isEqualTo("userId");
        assertThat(parsedClaims.get("email")).isEqualTo("test@example.com");

        verify(jwtConfig).getAccessTokenExpiration();
        verify(jwtConfig).getSecretKey();
        verify(userRoleRepository).findByUserUserId("userId");
    }

    @Test
    @DisplayName("Should handle null UserProfile when generating refresh token")
    void generateRefreshToken_WhenUserHasNullUserProfile_ShouldHandleNullProfile() {
        // Arrange
        User user = createTestUser();
        user.setUserProfile(null); // Set profile to null

        int tokenExpiration = 604800;
        SecretKey secretKey = createTestSecretKey();

        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(tokenExpiration);
        when(jwtConfig.getSecretKey()).thenReturn(secretKey);
        when(userRoleRepository.findByUserUserId("userId")).thenReturn(createTestUserRoles());

        // Act
        String result = jwtService.generateRefreshToken(user);

        // Assert
        assertThat(result).isNotNull().isNotEmpty();

        // Verify the token can be parsed and has null values for firstName/lastName
        var parsedClaims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(result)
                .getPayload();

        assertThat(parsedClaims.get("firstName")).isNull();
        assertThat(parsedClaims.get("lastName")).isNull();
        assertThat(parsedClaims.getSubject()).isEqualTo("userId");

        verify(jwtConfig).getRefreshTokenExpiration();
        verify(jwtConfig).getSecretKey();
        verify(userRoleRepository).findByUserUserId("userId");
    }

    @Test
    @DisplayName("Should validate token successfully when token is valid")
    void validateToken_WhenValidToken_ShouldReturnTrue() {
        // Arrange
        SecretKey secretKey = createTestSecretKey();
        String validToken = generateValidTestToken(secretKey);

        when(jwtConfig.getSecretKey()).thenReturn(secretKey);

        // Act
        boolean result = jwtService.validateToken(validToken);

        // Assert
        assertThat(result).isTrue();
        verify(jwtConfig).getSecretKey();
    }

    @Test
    @DisplayName("Should invalidate token when token is expired")
    void validateToken_WhenExpiredToken_ShouldReturnFalse() {
        // Arrange
        SecretKey secretKey = createTestSecretKey();
        String expiredToken = generateExpiredTestToken(secretKey);

        when(jwtConfig.getSecretKey()).thenReturn(secretKey);

        // Act
        boolean result = jwtService.validateToken(expiredToken);

        // Assert
        assertThat(result).isFalse();
        verify(jwtConfig).getSecretKey();
    }

    @Test
    @DisplayName("Should invalidate token when token is invalid")
    void validateToken_WhenInvalidToken_ShouldReturnFalse() {
        // Arrange
        String invalidToken = "invalid.token.here";
        SecretKey secretKey = createTestSecretKey();

        when(jwtConfig.getSecretKey()).thenReturn(secretKey);

        // Act
        boolean result = jwtService.validateToken(invalidToken);

        // Assert
        assertThat(result).isFalse();
        verify(jwtConfig).getSecretKey();
    }

    @Test
    @DisplayName("Should extract userId successfully from valid token")
    void extractUserId_WhenValidToken_ShouldReturnUserId() {
        // Arrange
        SecretKey secretKey = createTestSecretKey();
        String validToken = generateValidTestToken(secretKey);

        when(jwtConfig.getSecretKey()).thenReturn(secretKey);

        // Act
        String result = jwtService.extractUserId(validToken);

        // Assert
        assertThat(result).isEqualTo("userId");
        verify(jwtConfig).getSecretKey();
    }

    @Test
    @DisplayName("Should extract roles successfully from valid token")
    void extractRoles_WhenValidToken_ShouldReturnRoles() {
        // Arrange
        SecretKey secretKey = createTestSecretKey();
        String validToken = generateValidTestToken(secretKey);

        when(jwtConfig.getSecretKey()).thenReturn(secretKey);

        // Act
        List<String> result = jwtService.extractRoles(validToken);

        // Assert
        assertThat(result).containsExactly("USER", "ADMIN");
        verify(jwtConfig).getSecretKey();
    }

    @Test
    @DisplayName("Should extract email successfully from valid token")
    void extractEmail_WhenValidToken_ShouldReturnEmail() {
        // Arrange
        SecretKey secretKey = createTestSecretKey();
        String validToken = generateValidTestToken(secretKey);

        when(jwtConfig.getSecretKey()).thenReturn(secretKey);

        // Act
        String result = jwtService.extractEmail(validToken);

        // Assert
        assertThat(result).isEqualTo("test@example.com");
        verify(jwtConfig).getSecretKey();
    }

    @Test
    @DisplayName("Should extract firstName successfully from valid token")
    void extractFirstName_WhenValidToken_ShouldReturnFirstName() {
        // Arrange
        SecretKey secretKey = createTestSecretKey();
        String validToken = generateValidTestToken(secretKey);

        when(jwtConfig.getSecretKey()).thenReturn(secretKey);

        // Act
        String result = jwtService.extractFirstName(validToken);

        // Assert
        assertThat(result).isEqualTo("John");
        verify(jwtConfig).getSecretKey();
    }

    @Test
    @DisplayName("Should extract lastName successfully from valid token")
    void extractLastName_WhenValidToken_ShouldReturnLastName() {
        //Arrange
        SecretKey secretKey = createTestSecretKey();
        String validToken = generateValidTestToken(secretKey);

        when(jwtConfig.getSecretKey()).thenReturn(secretKey);

        // Act
        String result = jwtService.extractLastName(validToken);

        // Assert
        assertThat(result).isEqualTo("Doe");
        verify(jwtConfig).getSecretKey();
    }

    // Helper methods
    private User createTestUser() {
        User user = new User();
        user.setUserId("userId");
        user.setEmail("test@example.com");

        UserProfile profile = new UserProfile();
        profile.setFirstName("John");
        profile.setLastName("Doe");
        user.setUserProfile(profile);

        return user;
    }

    private List<UserRole> createTestUserRoles() {
        UserRole userRole = new UserRole();
        userRole.setRole(Role.USER);

        UserRole adminRole = new UserRole();
        adminRole.setRole(Role.ADMIN);

        return List.of(userRole, adminRole);
    }

    private SecretKey createTestSecretKey() {
        return Jwts.SIG.HS256.key().build();
    }

    private String generateValidTestToken(SecretKey secretKey) {
        return Jwts.builder()
                .subject("userId")
                .claim("email", "test@example.com")
                .claim("firstName", "John")
                .claim("lastName", "Doe")
                .claim("roles", List.of("USER", "ADMIN"))
                .issuer(String.valueOf(new Date()))
                .expiration(new Date(System.currentTimeMillis() + 3600 * 1000))
                .signWith(secretKey)
                .compact();
    }

    private String generateExpiredTestToken(SecretKey secretKey) {
        return Jwts.builder()
                .subject("userId")
                .claim("email", "test@example.com")
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(secretKey)
                .compact();
    }
}

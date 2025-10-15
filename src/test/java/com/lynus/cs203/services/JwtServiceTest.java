package com.lynus.cs203.services;

import com.lynus.cs203.config.JwtConfig;
import com.lynus.cs203.entities.Role;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.entities.UserProfile;
import com.lynus.cs203.entities.UserRole;
import com.lynus.cs203.repositories.UserRoleRepository;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private JwtService jwtService;

    @Test
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

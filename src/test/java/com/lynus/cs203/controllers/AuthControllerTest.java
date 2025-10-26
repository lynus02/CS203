package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.LoginRequest;
import com.lynus.cs203.dtos.response.JwtResponse;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.services.AuthService;
import com.lynus.cs203.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Test")
public class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    private final String testUserId = "userId";
    private final String testEmail = "test@example.com";
    private final String testRefreshToken = "refreshToken";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthentication() {
        when(authentication.getPrincipal()).thenReturn(testUserId);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_ShouldLoginSuccessfully_WithValidCredentials() {
        // Given
        LoginRequest loginRequest = createLoginRequest();
        JwtResponse expectedResponse = mockJwtResponse();

        when(authService.authenticateUser(loginRequest, httpServletResponse))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<JwtResponse> response = authController.login(loginRequest, httpServletResponse);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponse);

        // Verify
        verify(authService).authenticateUser(loginRequest, httpServletResponse);
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Should handle authentication service exception during login")
    void login_ShouldHandleException_DuringAuthentication() {
        // Given
        LoginRequest loginRequest = createLoginRequest();

        when(authService.authenticateUser(loginRequest, httpServletResponse))
                .thenThrow(new RuntimeException("Authentication failed"));

        assertThatThrownBy(() -> authController.login(loginRequest, httpServletResponse))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Authentication failed");
    }

    @Test
    @DisplayName("Should return current user profile")
    void me_ShouldReturnCurrentUserProfile() {
        // Given
        mockAuthentication();
        UserDto expectedUserDto = mockUserDto();

        when(userService.getUserByIdAsDto(eq(testUserId)))
                .thenReturn(expectedUserDto);

        // When
        ResponseEntity<UserDto> response = authController.me(httpServletRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedUserDto);
        assertThat(response.getBody().getUserId()).isEqualTo(testUserId);
        assertThat(response.getBody().getEmail()).isEqualTo(testEmail);

        // Verify
        verify(userService).getUserByIdAsDto(testUserId);
        verify(authentication).getPrincipal();
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Should handle user bot found when getting current user profile")
    void me_ShouldHandleUserNotFound_WhenGettingCurrentUserProfile() {
        // Given
        mockAuthentication();

        when(userService.getUserByIdAsDto(testUserId))
                .thenThrow(new RuntimeException("User not found"));

        // When / Then
        assertThatThrownBy(() -> authController.me(httpServletRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        // Verify
        verify(userService).getUserByIdAsDto(testUserId);
        verify(authentication).getPrincipal();
    }

    @Test
    @DisplayName("Should handle missing authentication when getting current user profile")
    void me_ShouldHandleMissingAuthentication_WhenGettingCurrentUserProfile() {
        // Given
        SecurityContextHolder.clearContext();

        // When / Then
        assertThatThrownBy(() -> authController.me(httpServletRequest))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should handle null principal when getting current user profile")
    void me_ShouldHandleNullPrincipal_WhenGettingCurrentUserProfile() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getPrincipal()).thenReturn(null);

        when(userService.getUserByIdAsDto(null)).thenReturn(null);

        ResponseEntity<UserDto> response = authController.me(httpServletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();

        verify(userService).getUserByIdAsDto(null);
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void refreshToken_ShouldRefreshTokenSuccessfully() {
        // Given
        JwtResponse expectedResponse = mockJwtResponse();

        when(authService.refreshAccessToken(testRefreshToken))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<JwtResponse> response = authController.refreshToken(testRefreshToken);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponse);

        // Verify
        verify(authService).refreshAccessToken(testRefreshToken);
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Should handle invalid refresh token")
    void refreshToken_ShouldHandleInvalidRefreshToken() {
        // Given
        when(authService.refreshAccessToken(testRefreshToken))
                .thenThrow(new RuntimeException("Invalid refresh token"));

        // When / Then
        assertThatThrownBy(() -> authController.refreshToken(testRefreshToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid refresh token");

        // Verify
        verify(authService).refreshAccessToken(testRefreshToken);
    }

    @Test
    @DisplayName("Should handle expired refresh token")
    void refreshToken_ShouldHandleExpiredRefreshToken() {
        // Given
        when(authService.refreshAccessToken(testRefreshToken))
                .thenThrow(new RuntimeException("Refresh token expired"));

        // When / Then
        assertThatThrownBy(() -> authController.refreshToken(testRefreshToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Refresh token expired");

        // Verify
        verify(authService).refreshAccessToken(testRefreshToken);
    }

    @Test
    @DisplayName("Should handle null refresh token")
    void refreshToken_ShouldHandleNullRefreshToken() {
        // Given
        String nullRefreshToken = null;

        when(authService.refreshAccessToken(nullRefreshToken))
                .thenThrow(new IllegalArgumentException("Refresh token is required"));

        // When / Then
        assertThatThrownBy(() -> authController.refreshToken(nullRefreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh token is required");

        // Verify
        verify(authService).refreshAccessToken(nullRefreshToken);
    }

    @Test
    @DisplayName("Should handle empty refresh token")
    void refreshToken_ShouldHandleEmptyRefreshToken() {
        // Given
        String emptyRefreshToken = "";

        when(authService.refreshAccessToken(emptyRefreshToken))
                .thenThrow(new IllegalArgumentException("Refresh token is required"));

        // When / Then
        assertThatThrownBy(() -> authController.refreshToken(emptyRefreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh token is required");

        // Verify
        verify(authService).refreshAccessToken(emptyRefreshToken);
    }

    @Test
    @DisplayName("Should handle service returning null responses")
    void refreshToken_shouldHandleServiceReturningNullResponses() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getPrincipal()).thenReturn(testUserId);

        when(userService.getUserByIdAsDto(testUserId))
                .thenReturn(null);

        ResponseEntity<UserDto> response = authController.me(httpServletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();

        verify(userService).getUserByIdAsDto(testUserId);
    }

    @Test
    @DisplayName("Should handle different user ID types in authentication principal")
    void me_shouldHandleServiceReturningNullResponses() {
        // Given
        Long longUserId = 123L;
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(authentication.getPrincipal()).thenReturn(longUserId);

        assertThatThrownBy(() -> authController.me(httpServletRequest))
                .isInstanceOf(ClassCastException.class);

        verifyNoInteractions(userService);
    }

    // Helper Methods
    private LoginRequest createLoginRequest() {
        return LoginRequest.builder()
                .email(testEmail)
                .password("Password@123")
                .build();
    }

    private JwtResponse mockJwtResponse() {
        return JwtResponse.builder()
                .token("mockJwtToken")
                .build();
    }

    private UserDto mockUserDto() {
        return UserDto.builder()
                .userId(testUserId)
                .email(testEmail)
                .firstName("John")
                .lastName("Doe")
                .build();
    }
}

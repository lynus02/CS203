package com.lynus.cs203.services;

import com.lynus.cs203.dtos.request.LoginRequest;
import com.lynus.cs203.dtos.response.JwtResponse;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.exceptions.UserNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Test")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private CookieService cookieService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should authenticate user and return JWT response when valid credentials are provided")
    void authenticateUser_WhenValidCredentials_ShouldReturnJwtResponse() {
        // Arrange
        String email = "test@example.com";
        String password = "Password@123";
        String accessToken = "access.token.here";
        String refreshToken = "refresh.token.here";

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        User user = new User();
        user.setUserId("userId");
        user.setEmail(email);

        when(userService.getUserByEmail(email)).thenReturn(user);
        when(jwtService.generateAccessToken(user)).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(user)).thenReturn(refreshToken);

        // Act
        JwtResponse result = authService.authenticateUser(request, response);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo(accessToken);

        // Verify
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService).getUserByEmail(email);
        verify(jwtService).generateAccessToken(user);
        verify(jwtService).generateRefreshToken(user);
        verify(cookieService).setRefreshTokenCookie(response, refreshToken);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when invalid credentials are provided")
    void authenticateUser_WhenInvalidCredentials_ShouldThrowException() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongPassword")
                .build();

        User user = new User();
        user.setUserId("userId");
        user.setEmail("test@example.com");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        when(userService.getUserByEmail(request.getEmail())).thenReturn(user);

        // Act & Assert
        assertThatThrownBy(() -> authService.authenticateUser(request, response))
                .isInstanceOf(BadCredentialsException.class);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService).getUserByEmail(anyString());
        verify(jwtService, never()).generateAccessToken(any());
        verify(jwtService, never()).generateRefreshToken(any());
        verify(cookieService, never()).setRefreshTokenCookie(any(), anyString());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user does not exist")
    void authenticateUser_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("Password@123")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        when(userService.getUserByEmail(request.getEmail()))
                .thenThrow(new UserNotFoundException("User not found with email: " + request.getEmail()));

        // Act & Assert
        assertThatThrownBy(() -> authService.authenticateUser(request, response))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
        verify(userService).getUserByEmail(request.getEmail());
        verify(jwtService, never()).generateAccessToken(any());
        verify(jwtService, never()).generateRefreshToken(any());
        verify(cookieService, never()).setRefreshTokenCookie(any(), anyString());
    }

    @Test
    @DisplayName("Should refresh access token when valid refresh token is provided")
    void refreshAccessToken_WhenValidRefreshToken_ShouldReturnNewAccessToken() {
        // Arrange
        String refreshToken = "valid.refresh.token";
        String userId = "userId";
        String newAccessToken = "new.access.token";

        User user = new User();
        user.setUserId(userId);

        when(jwtService.validateToken(refreshToken)).thenReturn(true);
        when(jwtService.extractUserId(refreshToken)).thenReturn(userId);
        when(userService.getUserById(userId)).thenReturn(user);
        when(jwtService.generateAccessToken(user)).thenReturn(newAccessToken);

        // Act
        JwtResponse result = authService.refreshAccessToken(refreshToken);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo(newAccessToken);

        // Verify
        verify(jwtService).validateToken(refreshToken);
        verify(jwtService).extractUserId(refreshToken);
        verify(userService).getUserById(userId);
        verify(jwtService).generateAccessToken(user);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when refresh token is null")
    void refreshAccessToken_WhenNullRefreshToken_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> authService.refreshAccessToken(null))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token is required");

        verify(jwtService, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when refresh token is empty")
    void refreshAccessToken_WhenEmptyRefreshToken_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> authService.refreshAccessToken(""))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token is required");

        verify(jwtService, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when refresh token is invalid")
    void refreshAccessToken_WhenInvalidRefreshToken_ShouldThrowException() {
        // Arrange
        String invalidRefreshToken = "invalid.refresh.token";

        when(jwtService.validateToken(invalidRefreshToken)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshAccessToken(invalidRefreshToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(jwtService).validateToken(invalidRefreshToken);
        verify(jwtService, never()).extractUserId(anyString());
        verify(userService, never()).getUserById(anyString());
        verify(jwtService, never()).generateAccessToken(any());
    }
}

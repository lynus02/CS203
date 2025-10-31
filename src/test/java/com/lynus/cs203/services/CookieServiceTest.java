package com.lynus.cs203.services;

import com.lynus.cs203.config.JwtConfig;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CookieService Unit Tests")
class CookieServiceTest {

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private CookieService cookieService;

    @Test
    @DisplayName("Should set refresh token cookie with correct attributes")
    void setRefreshTokenCookie_WhenValidRefreshToken_ShouldSetCookie() {
        // Arrange
        String refreshToken = "refresh.token.here";
        int refreshTokenExpiration = 604800; // 7 days

        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(refreshTokenExpiration);

        ArgumentCaptor<String> cookieCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        cookieService.setRefreshTokenCookie(response, refreshToken);

        // Assert
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), cookieCaptor.capture());
        verify(jwtConfig).getRefreshTokenExpiration();

        String cookieValue = cookieCaptor.getValue();
        assertThat(cookieValue).contains("refreshToken=" + refreshToken);
        assertThat(cookieValue).contains("HttpOnly");
        assertThat(cookieValue).contains("Path=/auth/refresh");
        assertThat(cookieValue).contains("Max-Age=" + refreshTokenExpiration);
        assertThat(cookieValue).contains("Secure");
    }

    @Test
    @DisplayName("Should set refresh token cookie with null value")
    void setRefreshTokenCookie_WhenNullRefreshToken_ShouldSetCookieWithNull() {
        // Arrange
        String refreshToken = null;
        int refreshTokenExpiration = 604800;

        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(refreshTokenExpiration);

        ArgumentCaptor<String> cookieCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        cookieService.setRefreshTokenCookie(response, refreshToken);

        // Assert
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), cookieCaptor.capture());
        verify(jwtConfig).getRefreshTokenExpiration();

        String cookieValue = cookieCaptor.getValue();
        assertThat(cookieValue).contains("refreshToken=");
        assertThat(cookieValue).contains("HttpOnly");
        assertThat(cookieValue).contains("Path=/auth/refresh");
        assertThat(cookieValue).contains("Secure");
    }

    @Test
    @DisplayName("Should set refresh token cookie with empty value")
    void setRefreshTokenCookie_WhenEmptyRefreshToken_ShouldSetCookieWithEmpty() {
        // Arrange
        String refreshToken = "";
        int refreshTokenExpiration = 604800;

        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(refreshTokenExpiration);

        ArgumentCaptor<String> cookieCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        cookieService.setRefreshTokenCookie(response, refreshToken);

        // Assert
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), cookieCaptor.capture());
        verify(jwtConfig).getRefreshTokenExpiration();

        String cookieValue = cookieCaptor.getValue();
        assertThat(cookieValue).contains("refreshToken=");
        assertThat(cookieValue).contains("HttpOnly");
        assertThat(cookieValue).contains("Path=/auth/refresh");
        assertThat(cookieValue).contains("Secure");
    }
}

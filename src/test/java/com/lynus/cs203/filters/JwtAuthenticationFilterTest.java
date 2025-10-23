package com.lynus.cs203.filters;

import com.lynus.cs203.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Jwt Authentication Filter Unit Test")
public class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);
    }

    @Test
    @DisplayName("Should continue filter chain when no Authorization header")
    void shouldContinueFilterChainWhenNoAuthorizationHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should continue filter chain when Authorization header has invalid format")
    void shouldContinueFilterChainWhenInvalidAuthorizationFormat() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("BearerTokenWithoutSpace");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should continue filter chain when Authorization header is invalid")
    void shouldContinueFilterChainWhenInvalidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("InvalidToken");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should continue filter chain when token validation fails")
    void shouldContinueFilterChainWhenTokenValidationFails() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
        when(jwtService.validateToken("invalid-token")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService).validateToken("invalid-token");
        verify(jwtService, never()).extractUserId(anyString());
        verify(jwtService, never()).extractRoles(anyString());
        verify(filterChain).doFilter(request, response);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("Should continue filter chain when userId is null")
    void shouldContinueFilterChainWhenUserIdIsNull() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.validateToken("valid-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-token")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService).validateToken("valid-token");
        verify(jwtService).extractUserId("valid-token");
        verify(jwtService, never()).extractRoles(anyString());
        verify(filterChain).doFilter(request, response);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("Should not set authentication when authentication already exists")
    void shouldNotSetAuthenticationWhenAuthenticationExists() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.validateToken("valid-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-token")).thenReturn("userId");

        // Pre-set an authentication in the SecurityContext
        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService).validateToken("valid-token");
        verify(jwtService).extractUserId("valid-token");
        verify(jwtService, never()).extractRoles("valid-token");
        verify(filterChain).doFilter(request, response);

        // Ensure the existing authentication was not replaced
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isEqualTo(existingAuth);
    }

    @Test
    @DisplayName("Should continue filter chain when token is valid and set authentication")
    void shouldContinueFilterChainWhenTokenIsValid() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.validateToken("valid-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-token")).thenReturn("userId");
        when(jwtService.extractRoles("valid-token")).thenReturn(List.of("USER"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService).validateToken("valid-token");
        verify(jwtService).extractUserId("valid-token");
        verify(jwtService).extractRoles("valid-token");
        verify(filterChain).doFilter(request, response);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo("userId");
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_USER");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}

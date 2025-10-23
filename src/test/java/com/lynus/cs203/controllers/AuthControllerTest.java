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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getPrincipal()).thenReturn(testUserId);
        SecurityContextHolder.setContext(securityContext);
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

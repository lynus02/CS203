package com.lynus.cs203.controllers;

import com.lynus.cs203.config.JwtConfig;
import com.lynus.cs203.dtos.request.LoginRequest;
import com.lynus.cs203.dtos.response.JwtResponse;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.services.JwtService;
import com.lynus.cs203.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        log.info("POST /auth/login - Login attempt for email: {}", request.getEmail());

        log.debug("Authenticating user credentials");
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        log.debug("Retrieving user details");
        User user = userService.getUserByEmail(request.getEmail());

        log.debug("Generating JWT token for user: {}", user.getUserId());
        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        // Create cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .path("/auth/refresh")
                .maxAge(jwtConfig.getRefreshTokenExpiration())    // 7 days
                .secure(true)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.info("Successful login for user: {} with email: {}", user.getUserId(), request.getEmail());
        return ResponseEntity.ok(new JwtResponse(accessToken));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(HttpServletRequest request) {
        log.info("GET /auth/me - Retrieving current user profile");

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = (String) authentication.getPrincipal();

        log.debug("Retrieving profile for authenticated user: {}", userId);
        UserDto user = userService.getUserByIdAsDto(userId);

        log.info("Successfully retrieved profile for user: {}", userId);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshToken(
            @CookieValue(value = "refreshToken") String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("Refresh token cookie is missing");
            throw new BadCredentialsException("Refresh token is required");
        }

        if (jwtService.validateToken(refreshToken)) {
            log.warn("Invalid refresh token");
            throw new BadCredentialsException("Invalid refresh token");
        }

        var userId = jwtService.extractUserId(refreshToken);
        User user = userService.getUserById(userId);
        var accessToken = jwtService.generateAccessToken(user);

        log.info("Successfully refreshed access token for user: {}", userId);
        return ResponseEntity.ok(new JwtResponse(accessToken));

    }

}

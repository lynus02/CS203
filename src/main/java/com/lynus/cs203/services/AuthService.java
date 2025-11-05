package com.lynus.cs203.services;

import com.lynus.cs203.exceptions.UnauthorizedException;
import com.lynus.cs203.dtos.request.LoginRequest;
import com.lynus.cs203.dtos.response.JwtResponse;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.exceptions.UserNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final CookieService cookieService;

    public JwtResponse authenticateUser(
            LoginRequest request,
            HttpServletResponse response
    ) {
        log.info("Authenticating user with email: {}", request.getEmail());

        authenticateCredentials(request);

        User user = userService.getUserByEmail(request.getEmail());
        log.debug("Retrieved user details for: {} (ID: {})", request.getEmail(), user.getUserId());

        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        log.debug("Generated JWT tokens for user: {}", user.getUserId());

        cookieService.setRefreshTokenCookie(response, refreshToken);
        log.info("User authenticated successfully: {} (ID: {})", request.getEmail(), user.getUserId());

        return new JwtResponse(accessToken);
    }

    public JwtResponse refreshAccessToken(String refreshToken) {
        log.info("Processing access token refresh request");

        validateRefreshToken(refreshToken);

        String userId = jwtService.extractUserId(refreshToken);
        log.debug("Extracted user ID from refresh token: {}", userId);

        User user = userService.getUserById(userId);
        String accessToken = jwtService.generateAccessToken(user);

        log.info("Access token refreshed successfully for user: {}", userId);
        return new JwtResponse(accessToken);
    }

    private void authenticateCredentials(LoginRequest request) {
        log.debug("Authenticating credentials for email: {}", request.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            log.debug("Credentials validated successfully for email: {}", request.getEmail());

        } catch (DisabledException e) {
            log.warn("Authentication failed - Account disabled for email: {}", request.getEmail());
            throw new UnauthorizedException("User account is disabled");

        } catch (BadCredentialsException e) {
            log.warn("Authentication failed - Invalid credentials for email: {}", request.getEmail());

            // Check if user exists to provide specific error message
            try {
                User user = userService.getUserByEmail(request.getEmail());
                log.debug("User exists but password is incorrect for email: {}", request.getEmail());
                // Re-throw the original exception since password is wrong
                throw new BadCredentialsException("Invalid password");
            } catch (UserNotFoundException ex) {
                log.warn("Authentication failed - User not found for email: {}", request.getEmail());
                throw ex;
            }
        }
    }

    private void validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("Refresh token validation failed - Token is missing or empty");
            throw new BadCredentialsException("Refresh token is required");
        }

        if (!jwtService.validateToken(refreshToken)) {
            log.warn("Refresh token validation failed - Token is invalid or expired");
            throw new BadCredentialsException("Invalid refresh token");
        }
        log.debug("Refresh token validated successfully");
    }
}

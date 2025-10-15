package com.lynus.cs203.services;

import com.lynus.cs203.config.JwtConfig;
import com.lynus.cs203.dtos.request.LoginRequest;
import com.lynus.cs203.dtos.response.JwtResponse;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.exceptions.UserNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
        log.info("Authenticating user");
        authenticateCredentials(request);

        log.debug("Retrieving user details");
        User user = userService.getUserByEmail(request.getEmail());

        log.debug("Generating JWT token for user: {}", user.getUserId());
        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        cookieService.setRefreshTokenCookie(response, refreshToken);

        return new JwtResponse(accessToken);
    }

    public JwtResponse refreshAccessToken(String refreshToken) {
        log.info("Refreshing access token for user");
        validateRefreshToken(refreshToken);

        String userId = jwtService.extractUserId(refreshToken);
        User user = userService.getUserById(userId);
        String accessToken = jwtService.generateAccessToken(user);

        return new JwtResponse(accessToken);
    }

    private void authenticateCredentials(LoginRequest request) {
        log.debug("Authenticating user credentials for: {}", request.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            User user = userService.getUserByEmail(request.getEmail());
            if (user == null) {
                log.warn("Authentication failed: User not found for email {}", request.getEmail());
                throw new UserNotFoundException("User not found");
            }
            throw e;
        }
    }

    private void validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("Refresh token cookie is missing");
            throw new BadCredentialsException("Refresh token is required");
        }

        if (!jwtService.validateToken(refreshToken)) {
            log.warn("Invalid refresh token");
            throw new BadCredentialsException("Invalid refresh token");
        }
    }
}

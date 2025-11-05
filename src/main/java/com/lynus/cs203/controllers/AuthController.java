package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.LoginRequest;
import com.lynus.cs203.dtos.response.JwtResponse;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.services.AuthService;
import com.lynus.cs203.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Authentication and user session management")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin
@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    private final AuthService authService;

    @Operation(
            summary = "User login",
            description = "Authenticate user credentials and return JWT tokens"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        log.info("Login attempt for email: {}", request.getEmail());

        JwtResponse jwtResponse = authService.authenticateUser(request, response);

        log.info("Successful login for user: {}", request.getEmail());
        return ResponseEntity.ok(jwtResponse);
    }

    @Operation(
            summary = "Get current user profile",
            description = "Retrieve the profile information of the currently authenticated user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = (String) authentication.getPrincipal();

        log.info("Retrieving current user profile for ID: {}", userId);

        UserDto user = userService.getUserByIdAsDto(userId);

        log.info("Successfully retrieved profile for user: {}", userId);
        return ResponseEntity.ok(user);
    }

    @Operation(
            summary = "Refresh access token",
            description = "Generate a new access token using a valid refresh token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Refresh token expired or invalid"),
            @ApiResponse(responseCode = "403", description = "Refresh token has been revoked")
    })
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshToken(
            @CookieValue(value = "refreshToken") String refreshToken
    ) {
        log.info("Refresh token attempt");

        JwtResponse jwtResponse = authService.refreshAccessToken(refreshToken);

        log.info("Successfully refreshed access token");
        return ResponseEntity.ok(jwtResponse);

    }
}

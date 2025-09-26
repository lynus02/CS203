package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.LoginRequest;
import com.lynus.cs203.dtos.response.ErrorResponse;
import com.lynus.cs203.dtos.response.JwtResponse;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.services.AuthService;
import com.lynus.cs203.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    private final AuthService authService;

    @Operation(
            summary = "User login",
            description = "Authenticate user credentials and return JWT tokens",
            security = {}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body or validation errors",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Parameter(
                    description = "User login credentials",
                    required = true,
                    schema = @Schema(implementation = LoginRequest.class))
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        log.info("POST /auth/login - Login attempt for email: {}", request.getEmail());

        JwtResponse jwtResponse = authService.authenticateUser(request, response);

        log.info("Successful login for user");
        return ResponseEntity.ok(jwtResponse);
    }

    @Operation(
            summary = "Get current user profile",
            description = "Retrieve the profile information of the currently authenticated user"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "User not authenticated or token invalid",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
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

    @Operation(
            summary = "Refresh access token",
            description = "Generate a new access token using a valid refresh token",
            security = {}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or invalid refresh token cookie",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token expired or invalid",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Refresh token has been revoked",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshToken(
            @Parameter(
                    description = "Refresh token cookie value (automatically sent by browser)",
                    required = true,
                    example = "eyJhbGciOiJIUzI1NiJ9...",
                    schema = @Schema(type = "string", format = "jwt"))
            @CookieValue(value = "refreshToken") String refreshToken
    ) {
        log.info("POST /auth/refresh - Refresh attempt for refresh token: {}", refreshToken);

        JwtResponse jwtResponse = authService.refreshAccessToken(refreshToken);

        log.info("Successfully refreshed access token for user");
        return ResponseEntity.ok(jwtResponse);

    }

}

package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.ChangePasswordRequest;
import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.request.UpdateUserRequest;
import com.lynus.cs203.dtos.response.ErrorResponse;
import com.lynus.cs203.dtos.response.PasswordChangeResponse;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.entities.Role;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.exceptions.EmailAlreadyExistsException;
import com.lynus.cs203.exceptions.InvalidPasswordException;
import com.lynus.cs203.exceptions.UserNotFoundException;
import com.lynus.cs203.mappers.UserMapper;
import com.lynus.cs203.repositories.UserRepository;
import com.lynus.cs203.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "User Management", description = "User management and profile operations")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    // Helper method to get authenticated user ID
    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return (String) authentication.getPrincipal();
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
                    description = "User not authenticated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getCurrentUserProfile() {
        log.info("GET /users/profile - Retrieving current user profile");

        String userId = getCurrentUserId();
        UserDto user = userService.getUserByIdAsDto(userId);

        log.info("Successfully retrieved profile for current user: {}", userId);
        return ResponseEntity.ok(user);
    }

    @Operation(
            summary = "Create new user account",
            description = "Register a new user account in the system"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body or validation errors",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email already exists",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @Parameter(
                    description = "User creation details",
                    required = true,
                    schema = @Schema(implementation = CreateUserRequest.class))
            @Valid @RequestBody CreateUserRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        log.info("POST /users - Creating new user with email: {}", request.getEmail());

        UserDto userDto = userService.createUserAsDto(request);
        var uri = uriBuilder.path("/users/{id}").buildAndExpand(userDto.getUserId()).toUri();

        log.info("Successfully created user with ID: {} for email: {}", userDto.getUserId(), request.getEmail());
        return  ResponseEntity.created(uri).body(userDto);
    }

    @Operation(
            summary = "Update current user profile",
            description = "Update profile information for the currently authenticated user"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body or validation errors",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "User not authenticated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email already exists",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateCurrentUserProfile(
            @Parameter(
                    description = "User profile update details",
                    required = true,
                    schema = @Schema(implementation = UpdateUserRequest.class))
            @Valid @RequestBody UpdateUserRequest request
    ) {
        log.info("PUT /users/profile - Updating current user profile");

        String userId = getCurrentUserId();
        UserDto userDto = userService.updateUserAsDto(userId, request);

        log.info("Successfully updated profile for current user: {}", userId);
        return ResponseEntity.ok(userDto);
    }

    @Operation(
            summary = "Delete current user account",
            description = "Permanently delete the currently authenticated user's account"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "User account deleted successfully"),
            @ApiResponse(
                    responseCode = "401",
                    description = "User not authenticated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/profile")
    public ResponseEntity<Void> deleteCurrentUser() {
        log.info("DELETE /users/profile - Deleting current user");

        String userId = getCurrentUserId();
        userService.deleteUser(userId);

        log.info("Successfully deleted current user: {}", userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Change user password",
            description = "Change the password for the currently authenticated user"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password changed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PasswordChangeResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body or validation errors",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "User not authenticated or invalid current password",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/change-password")
    public ResponseEntity<PasswordChangeResponse> changePassword(
            @Parameter(
                    description = "Password change details including current and new password",
                    required = true,
                    schema = @Schema(implementation = ChangePasswordRequest.class))
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        log.info("POST /users/change-password - Changing password for current user");

        String userId = getCurrentUserId();
        PasswordChangeResponse response = userService.changePassword(userId, request);

        log.info("Successfully changed password for current user: {}", userId);
        return ResponseEntity.ok(response);
    }

}

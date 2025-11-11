package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.ChangePasswordRequest;
import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.request.UpdateUserRequest;
import com.lynus.cs203.dtos.response.PasswordChangeResponse;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.entities.SavedProductConfig;
import com.lynus.cs203.services.UserService;
import io.jsonwebtoken.security.Request;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Tag(name = "User Management", description = "User management and profile operations")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    // Helper method to get authenticated user ID
    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof String) {
            return (String) principal;
        } else if (principal instanceof org.springframework.security.core.userdetails.User) {
            return ((org.springframework.security.core.userdetails.User) principal).getUsername();
        } else {
            throw new IllegalStateException("Unexpected principal type: " + principal.getClass().getName());
        }
    }

    @Operation(
            summary = "Get current user profile",
            description = "Retrieve the profile information of the currently authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getCurrentUserProfile() {
        String userId = getCurrentUserId();
        log.info("Retrieving current user profile for ID: {}", userId);

        UserDto user = userService.getUserByIdAsDto(userId);
        log.info("Successfully retrieved profile for user: {}", userId);

        return ResponseEntity.ok(user);
    }

    @Operation(
            summary = "Create new user account",
            description = "Register a new user account in the system"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody CreateUserRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        log.info("Creating new user with email: {}", request.getEmail());

        UserDto userDto = userService.createUserAsDto(request);
        var uri = uriBuilder.path("/users/{id}").buildAndExpand(userDto.getUserId()).toUri();

        log.info("Successfully created user with ID: {} for email: {}", userDto.getUserId(), request.getEmail());
        return ResponseEntity.created(uri).body(userDto);
    }

    @Operation(
            summary = "Update current user profile",
            description = "Update profile information for the currently authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateCurrentUserProfile(
            @Valid @RequestBody UpdateUserRequest request
    ) {
        String userId = getCurrentUserId();
        log.info("Updating profile for current user: {}", userId);

        UserDto userDto = userService.updateUserAsDto(userId, request);
        log.info("Successfully updated profile for user: {}", userId);

        return ResponseEntity.ok(userDto);
    }

    @Operation(
            summary = "Delete current user account",
            description = "Permanently delete the currently authenticated user's account"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User account deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/profile")
    public ResponseEntity<Void> deleteCurrentUser() {
        String userId = getCurrentUserId();
        log.info("Deleting current user: {}", userId);

        userService.deleteUser(userId);
        log.info("Successfully deleted user: {}", userId);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Change user password",
            description = "Change the password for the currently authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid current password"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/change-password")
    public ResponseEntity<PasswordChangeResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        String userId = getCurrentUserId();
        log.info("Changing password for current user: {}", userId);

        PasswordChangeResponse response = userService.changePassword(userId, request);
        log.info("Successfully changed password for user: {}", userId);

        return ResponseEntity.ok(response);
    }
    @Operation(summary = "Get saved products for current user")
    @GetMapping("/saved-products")
    public ResponseEntity<List<SavedProductConfig>> getSavedProducts() {
        String userId = getCurrentUserId();
        List<SavedProductConfig> savedProducts = userService.getSavedProducts(userId);
        log.info("Retrieving saved products for current user: {}", userId);
        return ResponseEntity.ok(savedProducts);
    }

    @Operation(summary = "Save a product for current user")
    @PostMapping("/saved-products")
    public ResponseEntity<SavedProductConfig> saveProduct(@RequestBody SavedProductConfig productData) {
        String userId = getCurrentUserId();
        SavedProductConfig savedProduct = userService.saveProduct(userId,productData);
        return ResponseEntity.ok(savedProduct);
    }

    @Operation(summary = "Delete a saved product for current user")
    @DeleteMapping("/saved-products/{productId}")
    public ResponseEntity<Void> deleteSavedProduct(@PathVariable String productId) {
        String userId = getCurrentUserId();
        userService.deleteSavedProduct(userId, productId);
        log.info("Deleted saved product {} for current user: {}", productId, userId);
        return ResponseEntity.noContent().build();
    }
}

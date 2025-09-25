package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.ChangePasswordRequest;
import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.request.UpdateUserRequest;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.entities.Role;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.exceptions.EmailAlreadyExistsException;
import com.lynus.cs203.exceptions.InvalidPasswordException;
import com.lynus.cs203.exceptions.UserNotFoundException;
import com.lynus.cs203.mappers.UserMapper;
import com.lynus.cs203.repositories.UserRepository;
import com.lynus.cs203.services.UserService;
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

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getCurrentUserProfile() {
        log.info("GET /users/profile - Retrieving current user profile");

        String userId = getCurrentUserId();
        UserDto user = userService.getUserByIdAsDto(userId);

        log.info("Successfully retrieved profile for current user: {}", userId);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<?> createUser(
            @Valid @RequestBody CreateUserRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        log.info("POST /users - Creating new user with email: {}", request.getEmail());

        UserDto userDto = userService.createUserAsDto(request);
        var uri = uriBuilder.path("/users/{id}").buildAndExpand(userDto.getUserId()).toUri();

        log.info("Successfully created user with ID: {} for email: {}", userDto.getUserId(), request.getEmail());
        return  ResponseEntity.created(uri).body(userDto);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateCurrentUserProfile(
            @Valid @RequestBody UpdateUserRequest request
    ) {
        log.info("PUT /users/profile - Updating current user profile");

        String userId = getCurrentUserId();
        UserDto userDto = userService.updateUserAsDto(userId, request);

        log.info("Successfully updated profile for current user: {}", userId);
        return ResponseEntity.ok(userDto);
    }

    @DeleteMapping("/profile")
    public ResponseEntity<Void> deleteCurrentUser() {
        log.info("DELETE /users/profile - Deleting current user");

        String userId = getCurrentUserId();
        userService.deleteUser(userId);

        log.info("Successfully deleted current user: {}", userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        log.info("POST /users/change-password - Changing password for current user");

        String userId = getCurrentUserId();
        userService.changePassword(userId, request);

        log.info("Successfully changed password for current user: {}", userId);

        Map<String, String> response = Map.of(
                "message", "Password changed successfully"
        );
        return ResponseEntity.ok(response);
    }

}

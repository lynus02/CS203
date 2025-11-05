package com.lynus.cs203.services;

import com.lynus.cs203.dtos.request.CreateUserRequest;
import com.lynus.cs203.dtos.response.AdminCreationResponse;
import com.lynus.cs203.dtos.response.SetupStatusResponse;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.entities.Role;
import com.lynus.cs203.exceptions.AdminAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class SetupService {
    private final UserService userService;

    public AdminCreationResponse createFirstAdmin(CreateUserRequest request) {
        log.info("Attempting to create first admin user with email: {}", request.getEmail());

        if (userService.adminExists()) {
            log.warn("Admin creation blocked - admin user already exists for email: {}", request.getEmail());
            throw new AdminAlreadyExistsException("Admin setup already completed. Cannot create additional admin users.");
        }

        log.debug("No existing admin found, proceeding with admin creation");

        try {
            // Create user and assign admin role
            UserDto userDto = userService.createUserAsDto(request);
            log.debug("User created successfully, assigning ADMIN role to user: {}", userDto.getUserId());

            userService.assignRole(userDto.getUserId(), Role.ADMIN);

            log.info("Successfully created first admin user - ID: {}, Email: {}",
                    userDto.getUserId(), userDto.getEmail());

            return AdminCreationResponse.builder()
                    .message("First admin user created successfully")
                    .userId(userDto.getUserId())
                    .email(userDto.getEmail())
                    .build();

        } catch (AdminAlreadyExistsException e) {
            // Re-throw specific exceptions unchanged
            log.warn("Admin creation conflict detected during process: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to create first admin user for email: {} - Error: {}",
                    request.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to create admin user for: " + request.getEmail(), e);
        }
    }

    public SetupStatusResponse getSetupStatus() {
        log.debug("Checking system setup status");

        boolean adminExists = userService.adminExists();
        String statusMessage = adminExists ? "System setup completed - admin user exists"
                : "System setup pending - no admin user found";

        log.debug("Setup status check completed: {}", statusMessage);

        return SetupStatusResponse.builder()
                .setupComplete(adminExists)
                .message(statusMessage)
                .build();
    }
}

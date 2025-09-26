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
        if (userService.adminExists()) {
            log.warn("Attempted to create first admin but admin already exists");
            throw new AdminAlreadyExistsException("Admin setup already completed");
        }

        try {
            // Create user and assign admin role
            UserDto userDto = userService.createUserAsDto(request);
            userService.assignRole(userDto.getUserId(), Role.ADMIN);

            log.info("Successfully created first admin user: {}", userDto.getUserId());

            return AdminCreationResponse.builder()
                    .message("First admin user created successfully")
                    .userId(userDto.getUserId())
                    .email(userDto.getEmail())
                    .build();

        } catch (Exception e) {
            log.error("Failed to create first admin user: {}", e.getMessage());
            throw new RuntimeException("Failed to create admin user", e);
        }
    }

    public SetupStatusResponse getSetupStatus() {
        boolean adminExists = userService.adminExists();

        return SetupStatusResponse.builder()
                .setupComplete(adminExists)
                .message(adminExists ? "Admin user exists" : "No admin user found")
                .build();
    }
}

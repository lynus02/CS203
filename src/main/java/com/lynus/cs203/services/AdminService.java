package com.lynus.cs203.services;

import com.lynus.cs203.dtos.response.RoleOperationResponse;
import com.lynus.cs203.dtos.response.UserRolesResponse;
import com.lynus.cs203.entities.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdminService {
    private final UserService userService;
    private final RoleValidationService roleValidationService;

    public RoleOperationResponse assignRoleToUser(String userId, String roleName) {
        log.info("Assigning role {} to user ID: {}", roleName, userId);

        Role role = roleValidationService.validateAndGetRole(roleName);
        userService.assignRole(userId, role);

        log.info("Successfully assigned role '{}' to user ID: {}", roleName, userId);
        return RoleOperationResponse.builder()
                .message("Role assigned successfully")
                .userId(userId)
                .role(roleName.toUpperCase())
                .build();
    }

    public RoleOperationResponse removeRoleFromUser(String userId, String roleName) {
        log.info("Removing role '{}' from user ID: {}", roleName, userId);

        Role role = roleValidationService.validateAndGetRole(roleName);
        userService.removeRole(userId, role);

        log.info("Successfully removed role '{}' from user ID: {}", roleName, userId);
        return RoleOperationResponse.builder()
                .message("Role removed successfully")
                .userId(userId)
                .role(roleName.toUpperCase())
                .build();
    }

    public UserRolesResponse getUserRoles(String userId) {
        log.info("Retrieving roles for user ID: {}", userId);

        List<String> roles = userService.getUserRoles(userId);

        log.info("Retrieved {} roles for user ID: {} - Roles: {}", roles.size(), userId, roles);
        return UserRolesResponse.builder()
                .userId(userId)
                .roles(roles)
                .build();
    }
}

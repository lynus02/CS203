package com.lynus.cs203.services;

import com.lynus.cs203.dtos.response.RoleOperationResponse;
import com.lynus.cs203.dtos.response.UserRolesResponse;
import com.lynus.cs203.entities.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdminService {
    private final UserService userService;
    private final RoleValidationService roleValidationService;

    public RoleOperationResponse assignRoleToUser(String userId, String roleName) {
        log.info("Assigning role {} to user {}", roleName, userId);

        Role role = roleValidationService.validateAndGetRole(roleName);
        userService.assignRole(userId, role);

        return RoleOperationResponse.builder()
                .message("Role assigned successfully")
                .userId(userId)
                .role(roleName.toUpperCase())
                .build();
    }

    public RoleOperationResponse removeRoleFromUser(String userId, String roleName) {
        log.info("Removing role {} from user {}", roleName, userId);

        Role role = roleValidationService.validateAndGetRole(roleName);
        userService.removeRole(userId, role);

        return RoleOperationResponse.builder()
                .message("Role removed successfully")
                .userId(userId)
                .role(roleName.toUpperCase())
                .build();
    }

    public UserRolesResponse getUserRoles(String userId) {
        log.info("Getting roles for user {}", userId);

        List<String> roles = userService.getUserRoles(userId);

        return UserRolesResponse.builder()
                .userId(userId)
                .roles(roles)
                .build();
    }
}

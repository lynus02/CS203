package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.entities.Role;
import com.lynus.cs203.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(required = false, defaultValue = "name") String sort
    ) {
        log.info("GET /admin/users - Admin retrieving all users with sort parameter: {}", sort);

        List<UserDto> users = userService.getAllUsersAsDto(sort);
        log.info("Successfully retrieved {} users", users.size());

        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUser(
            @PathVariable String id
    ) {
        log.info("GET /admin/users/{} - Admin retrieving user by ID", id);

        UserDto user = userService.getUserByIdAsDto(id);

        log.info("Successfully retrieved user: {}", id);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users/{id}/roles/{roleName}")
    public ResponseEntity<Map<String, String>> assignRole(
            @PathVariable String id,
            @PathVariable String roleName
    ) {
        log.info("POST /admin/users/{}/roles/{} - Admin assigning role to user", id, roleName);

        try {
            Role role = Role.fromName(roleName.toUpperCase());
            userService.assignRole(id, role);

            Map<String, String> response = Map.of(
                    "message", "Role assigned successfully",
                    "userId", id,
                    "role", roleName.toUpperCase()
            );

            log.info("Successfully assigned role {} to user: {}", roleName, id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role name provided: {}", roleName);
            Map<String, String> errorResponse = Map.of(
                    "error", "Invalid role name: " + roleName,
                    "validRoles", "USER, ADMIN"
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping("/users/{id}/roles/{roleName}")
    public ResponseEntity<Map<String, String>> removeRole(
            @PathVariable String id,
            @PathVariable String roleName) {
        log.info("DELETE /admin/users/{}/roles/{} - Admin removing role from user", id, roleName);

        try {
            Role role = Role.fromName(roleName.toUpperCase());
            userService.removeRole(id, role);

            Map<String, String> response = Map.of(
                    "message", "Role removed successfully",
                    "userId", id,
                    "role", roleName.toUpperCase()
            );

            log.info("Successfully removed role {} from user: {}", roleName, id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role name provided: {}", roleName);
            Map<String, String> errorResponse = Map.of(
                    "error", "Invalid role name: " + roleName,
                    "validRoles", "USER, ADMIN"
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/users/{id}/roles")
    public ResponseEntity<Map<String, Object>> getUserRoles(@PathVariable String id) {
        log.info("GET /admin/users/{}/roles - Admin retrieving user roles", id);

        List<String> roles = userService.getUserRoles(id);

        Map<String, Object> response = Map.of(
                "userId", id,
                "roles", roles
        );

        log.info("Successfully retrieved roles for user: {}", id);
        return ResponseEntity.ok(response);
    }


}

package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.response.RoleOperationResponse;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.dtos.response.UserRolesResponse;
import com.lynus.cs203.services.AdminService;
import com.lynus.cs203.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Operations", description = "Administrative operations for user and role management")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {
    private final UserService userService;
    private final AdminService adminService;

    // Constants for consistent logging and documentation
    private static final String USER_ID_EXAMPLE = "123e4567-e89b-12d3-a456-426614174000";
    private static final String USER_ID_DESCRIPTION = "Unique identifier of the user (UUID)";

    @Operation(
            summary = "Get all users",
            description = "Retrieve all users in the system with optional sorting"
    )
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers(
            @Parameter(
                    description = "Field to sort users by",
                    examples = {
                            @ExampleObject(name = "By name", value = "name"),
                            @ExampleObject(name = "By email", value = "email"),
                            @ExampleObject(name = "By creation date", value = "createdAt")
                    }
            )
            @RequestParam(required = false, defaultValue = "name") String sort
    ) {
        log.info("Admin retrieving all users with sort parameter: '{}'", sort);

        List<UserDto> users = userService.getAllUsersAsDto(sort);
        log.debug("Retrieved {} users sorted by '{}'", users.size(), sort);

        return ResponseEntity.ok(users);
    }

    @Operation(
            summary = "Get user by ID",
            description = "Retrieve a specific user by their unique identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUser(
            @Parameter(
                    description = USER_ID_DESCRIPTION,
                    example = USER_ID_EXAMPLE)
            @PathVariable String id
    ) {
        log.info("Admin retrieving user by ID: {}", id);

        UserDto user = userService.getUserByIdAsDto(id);
        log.info("Successfully retrieved user ID: {}", id);

        return ResponseEntity.ok(user);
    }

    @Operation(
            summary = "Assign role to user",
            description = "Assign a specific role to a user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role assigned successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "User already has this role")
    })
    @PostMapping("/users/{id}/roles/{roleName}")
    public ResponseEntity<RoleOperationResponse> assignRole(
            @Parameter(
                    description = USER_ID_DESCRIPTION,
                    example = USER_ID_EXAMPLE)
            @PathVariable String id,
            @Parameter(
                    description = "Role name to assign to the user",
                    examples = {
                            @ExampleObject(name = "User Role", value = "USER"),
                            @ExampleObject(name = "Admin Role", value = "ADMIN")
                    })
            @PathVariable String roleName
    ) {
        log.info("Admin assigning role '{}' to user ID: {}", roleName, id);

        RoleOperationResponse response = adminService.assignRoleToUser(id, roleName);
        log.info("Successfully assigned role '{}' to user ID: {}", roleName, id);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Remove role from user",
            description = "Remove a specific role from a user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role removed successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "User doesn't have this role")
    })
    @DeleteMapping("/users/{id}/roles/{roleName}")
    public ResponseEntity<RoleOperationResponse> removeRole(
            @Parameter(
                    description = USER_ID_DESCRIPTION,
                    example = USER_ID_EXAMPLE)
            @PathVariable String id,
            @Parameter(
                    description = "Role name to remove from the user",
                    examples = {
                            @ExampleObject(name = "User Role", value = "USER"),
                            @ExampleObject(name = "Admin Role", value = "ADMIN")
                    })
            @PathVariable String roleName) {
        log.info("Admin removing role '{}' from user ID: {}", roleName, id);

        RoleOperationResponse response = adminService.removeRoleFromUser(id, roleName);
        log.info("Successfully removed role '{}' from user ID: {}", roleName, id);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get user roles",
            description = "Retrieve all roles assigned to a specific user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User roles retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/users/{id}/roles")
    public ResponseEntity<UserRolesResponse> getUserRoles(
            @Parameter(
                    description = USER_ID_DESCRIPTION,
                    example = USER_ID_EXAMPLE)
            @PathVariable String id
    ) {
        log.info("Admin retrieving roles for user ID: {}", id);

        UserRolesResponse response = adminService.getUserRoles(id);
        log.info("Retrieved {} roles for user ID: {}", response.getRoles().size(), id);

        return ResponseEntity.ok(response);
    }
}

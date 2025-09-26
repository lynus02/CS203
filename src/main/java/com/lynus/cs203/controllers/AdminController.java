package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.response.ErrorResponse;
import com.lynus.cs203.dtos.response.RoleOperationResponse;
import com.lynus.cs203.dtos.response.UserDto;
import com.lynus.cs203.dtos.response.UserRolesResponse;
import com.lynus.cs203.services.AdminService;
import com.lynus.cs203.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Operation(
            summary = "Get all users",
            description = "Retrieve all users in the system with optional sorting"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Users retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UserDto.class)))),
            @ApiResponse(
                    responseCode = "401",
                    description = "User not authenticated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Admin role required",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers(
            @Parameter(
                    description = "Sort parameter for users",
                    example = "name",
                    schema = @Schema(allowableValues = {"name", "email", "createdAt"}))
            @RequestParam(required = false, defaultValue = "name") String sort
    ) {
        log.info("GET /admin/users - Admin retrieving all users with sort parameter: {}", sort);

        List<UserDto> users = userService.getAllUsersAsDto(sort);
        log.info("Successfully retrieved {} users", users.size());

        return ResponseEntity.ok(users);
    }

    @Operation(
            summary = "Get user by ID",
            description = "Retrieve a specific user by their unique identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "User not authenticated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Admin role required",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUser(
            @Parameter(
                    description = "User ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String id
    ) {
        log.info("GET /admin/users/{} - Admin retrieving user by ID", id);

        UserDto user = userService.getUserByIdAsDto(id);

        log.info("Successfully retrieved user: {}", id);
        return ResponseEntity.ok(user);
    }

    @Operation(
            summary = "Assign role to user",
            description = "Assign a specific role to a user"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Role assigned successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RoleOperationResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid role name",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "User not authenticated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Admin role required",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "User already has this role",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/users/{id}/roles/{roleName}")
    public ResponseEntity<RoleOperationResponse> assignRole(
            @Parameter(
                    description = "User ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String id,
            @Parameter(
                    description = "Role name to assign",
                    required = true,
                    example = "ADMIN", schema = @Schema(allowableValues = {"USER", "ADMIN"}))
            @PathVariable String roleName
    ) {
        log.info("POST /admin/users/{}/roles/{} - Admin assigning role to user", id, roleName);

        RoleOperationResponse response = adminService.assignRoleToUser(id, roleName);
        log.info("Successfully assigned role {} to user: {}", roleName, id);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Remove role from user",
            description = "Remove a specific role from a user"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Role removed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RoleOperationResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid role name",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "User not authenticated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Admin role required",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "User doesn't have this role",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/users/{id}/roles/{roleName}")
    public ResponseEntity<RoleOperationResponse> removeRole(
            @Parameter(
                    description = "User ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String id,
            @Parameter(
                    description = "Role name to remove",
                    required = true,
                    example = "USER", schema = @Schema(allowableValues = {"USER", "ADMIN"}))
            @PathVariable String roleName) {
        log.info("DELETE /admin/users/{}/roles/{} - Admin removing role from user", id, roleName);

        RoleOperationResponse response = adminService.removeRoleFromUser(id, roleName);
        log.info("Successfully removed role {} from user: {}", roleName, id);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get user roles",
            description = "Retrieve all roles assigned to a specific user"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User roles retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserRolesResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "User not authenticated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Admin role required",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/users/{id}/roles")
    public ResponseEntity<UserRolesResponse> getUserRoles(
            @Parameter(
                    description = "User ID",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String id
    ) {
        log.info("GET /admin/users/{}/roles - Admin retrieving user roles", id);

        UserRolesResponse response = adminService.getUserRoles(id);

        log.info("Successfully retrieved roles for user: {}", id);
        return ResponseEntity.ok(response);
    }


}

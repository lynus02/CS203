package com.lynus.cs203.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@AllArgsConstructor
@Getter
@Schema(description = "Response object for user details")
public class UserDto {
    @Schema(
            description = "Unique identifier of the user",
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    private String userId;

    @Schema(
            description = "Email address of the user",
            example = "john.doe@example.com",
            format = "email"
    )
    private String email;

    @Schema(
            description = "First name of the user",
            example = "John"
    )
    private String firstName;

    @Schema(
            description = "Last name of the user",
            example = "Doe"
    )
    private String lastName;

    @Schema(
            description = "URL to user's avatar image (optional)",
            example = "https://example.com/avatars/user123.jpg",
            nullable = true
    )
    private String avatarUrl;

    @Schema(
            description = "Whether the user account is active",
            example = "true"
    )
    private Boolean isActive;

    @Schema(
            description = "Timestamp when the user was created",
            example = "2024-01-15T10:30:00",
            format = "date-time"
    )
    private LocalDateTime createdAt;

    @Schema(
            description = "Timestamp when the user was last updated",
            example = "2024-01-20T14:25:00",
            format = "date-time"
    )
    private LocalDateTime updatedAt;

    @Schema(
            description = "List of user roles",
            example = "[\"USER\", \"ADMIN\"]"
    )
    private List<String> roles;
}
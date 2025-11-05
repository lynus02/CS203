package com.lynus.cs203.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response object for admin creation")
public class AdminCreationResponse {
    @Schema(
            description = "Success message for admin creation",
            example = "Admin created successfully"
    )
    private String message;

    @Schema(
            description = "Unique identifier of the created admin user",
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    private String userId;


    @Schema(
            description = "Email address of the created admin",
            example = "admin@example.com"
    )
    private String email;
}

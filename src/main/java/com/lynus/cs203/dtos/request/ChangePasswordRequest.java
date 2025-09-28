package com.lynus.cs203.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request object for changing the user password")
public class ChangePasswordRequest {
    @Schema(
            description = "Current password of the user",
            example = "currentPassword123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Old password is required")
    private String oldPassword;

    @Schema(
            description = "New password to set (6-25 characters)",
            example = "newPassword456",
            minLength = 6,
            maxLength = 25,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 25, message = "Password must be between 6 and 25 characters")
    private String newPassword;
}

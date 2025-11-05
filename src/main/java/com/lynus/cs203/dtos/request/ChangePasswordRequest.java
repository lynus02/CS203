package com.lynus.cs203.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Schema(description = "Request object for changing the user password")
public class ChangePasswordRequest {
    @Schema(
            description = "Current password of the user",
            example = "currentPassword123"
    )
    @NotBlank(message = "Old password is required")
    private String oldPassword;

    @Schema(
            description = "New password to set (6-25 characters)",
            example = "SecurePass123!",
            minLength = 6,
            maxLength = 25
    )
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 25, message = "Password must be between 6 and 25 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,25}$",
            message = "Password must be at least 6 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character")
    private String newPassword;
}

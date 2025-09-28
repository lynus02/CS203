package com.lynus.cs203.dtos.request;

import com.lynus.cs203.validation.Lowercase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request object for creating a new user")
public class CreateUserRequest {
    @Schema(
            description = "First name of the user",
            example = "John",
            maxLength = 255,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "First name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String firstName;

    @Schema(
            description = "Last name of the user",
            example = "Doe",
            maxLength = 255,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Last name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String lastName;

    @Schema(
            description = "Email address (must be lowercase and valid)",
            example = "john.doe@example.com",
            format = "email",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Lowercase(message = "Email must be lowercase")
    private String email;

    @Schema(
            description = "Password for the account (6-25 characters)",
            example = "securePassword123",
            minLength = 6,
            maxLength = 25,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 25, message = "Password must be between 6 and 25 characters")
    private String password;
}

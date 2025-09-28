package com.lynus.cs203.dtos.request;

import com.lynus.cs203.validation.Lowercase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request object for updating user details")
public class UpdateUserRequest {
    @Schema(
            description = "First name of the user (optional)",
            example = "John",
            maxLength = 255,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String firstName;

    @Schema(
            description = "Last name of the user (optional)",
            example = "Doe",
            maxLength = 255,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String lastName;

    @Schema(
            description = "Email address (optional, must be valid and lowercase if provided)",
            example = "john.doe@example.com",
            format = "email",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Email(message = "Email must be valid")
    @Lowercase(message = "Email must be lowercase")
    private String email;
}

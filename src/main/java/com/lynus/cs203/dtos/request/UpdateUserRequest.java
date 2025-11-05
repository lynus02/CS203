package com.lynus.cs203.dtos.request;

import com.lynus.cs203.validation.Lowercase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Schema(description = "Request object for updating user details")
public class UpdateUserRequest {
    @Schema(
            description = "First name of the user (optional)",
            example = "John",
            minLength = 3,
            maxLength = 255
    )
    @Size(min = 3, max = 255, message = "First name must be between 3 to 255 characters")
    private String firstName;

    @Schema(
            description = "Last name of the user (optional)",
            example = "Doe",
            minLength = 3,
            maxLength = 255
    )
    @Size(min = 3, max = 255, message = "Last name must be between 3 to 255 characters")
    private String lastName;

    @Schema(
            description = "Email address (optional, must be valid and lowercase if provided)",
            example = "john.doe@example.com"
    )
    @Email(message = "Email must be valid")
    @Lowercase(message = "Email must be lowercase")
    private String email;
}

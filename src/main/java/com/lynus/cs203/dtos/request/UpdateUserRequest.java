package com.lynus.cs203.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}

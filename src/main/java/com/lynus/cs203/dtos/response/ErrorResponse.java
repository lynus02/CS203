package com.lynus.cs203.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response object for error handling")
public class ErrorResponse {
    @Schema(
            description = "HTTP status code",
            example = "400"
    )
    private int status;

    @Schema(
            description = "Error type or category",
            example = "Bad Request"
    )
    private String error;

    @Schema(
            description = "Detailed error message",
            example = "Validation failed for one or more fields"
    )
    private String message;

    @Schema(
            description = "Field-specific validation errors (optional)",
            example = "{\"email\": \"Email must be valid\", \"password\": \"Password is required\"}"
    )
    private Map<String, String> errors;

    @Schema(
            description = "API endpoint path where error occurred",
            example = "/api/users"
    )
    private String path;
}

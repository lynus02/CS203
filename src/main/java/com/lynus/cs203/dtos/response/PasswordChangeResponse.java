package com.lynus.cs203.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response object for password change")
public class PasswordChangeResponse {
    @Schema(
            description = "Success message for password change",
            example = "Password changed successfully"
    )
    private String message;

    @Schema(
            description = "Unique identifier of the user",
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    private String userId;
}

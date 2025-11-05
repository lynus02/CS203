package com.lynus.cs203.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response object for role assignment or removal")
public class RoleOperationResponse {
    @Schema(
            description = "Success message for role operation",
            example = "Role assigned successfully"
    )
    private String message;

    @Schema(
            description = "Unique identifier of the user",
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    private String userId;

    @Schema(
            description = "Role name that was assigned or removed",
            example = "ADMIN",
            allowableValues = {"USER", "ADMIN"}
    )
    private String role;
}

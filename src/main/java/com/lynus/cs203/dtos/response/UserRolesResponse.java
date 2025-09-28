package com.lynus.cs203.dtos.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Response object for user roles")
public class UserRolesResponse {
    @Schema(
            description = "Unique identifier of the user",
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    private String userId;

    @ArraySchema(
            schema = @Schema(
                    description = "Role name",
                    example = "ADMIN",
                    allowableValues = {"USER", "ADMIN"}
            )
    )
    private List<String> roles;
}

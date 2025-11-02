package com.lynus.cs203.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
@Schema(description = "Response object containing the JWT token")
public class JwtResponse {
    @Schema(
            description = "JWT Bearer token for authentication",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            format = "JWT"
    )
    private String token;
}

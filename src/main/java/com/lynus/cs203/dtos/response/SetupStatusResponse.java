package com.lynus.cs203.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response object for setup status")
public class SetupStatusResponse {
    @Schema(
            description = "Whether system setup is complete",
            example = "true"
    )
    private boolean setupComplete;

    @Schema(
            description = "Setup status message",
            example = "System setup completed successfully"
    )
    private String message;
}

package com.lynus.cs203.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Schema(description = "Request object for tariff calculation")
public class TariffCalculationRequest {
    @Schema(
            description = "Product code for tariff calculation",
            example = "80550",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Product code is required")
    private Integer productCode;

    @Schema(
            description = "Export Country code (C + 3 numeric characters)",
            example = "C840",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 4,
            maxLength = 4
    )
    @NotBlank(message = "Export country code is required")
    private String exportCountryCode;

    @Schema(
            description = "Destination Country code (C + 3 numeric characters)",
            example = "C840",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 4,
            maxLength = 4
    )
    @NotBlank(message = "Country code is required")
    private String desCountryCode;

    @Schema(
            description = "Customs value for tariff calculation",
            example = "1000.50",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Positive(message = "Customs value must be positive")
    private double customsValue;
}

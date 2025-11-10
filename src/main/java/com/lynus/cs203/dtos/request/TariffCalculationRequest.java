package com.lynus.cs203.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
@Schema(description = "Request object for tariff calculation")
public class TariffCalculationRequest {
    @Schema(
            description = "Product code for tariff calculation",
            example = "080550" // now a STRING
    )
    @NotBlank(message = "Product code is required")
    private String productCode;

    @Schema(
            description = "Export Country code (C + 3 numeric characters)",
            example = "C840",
            minLength = 4,
            maxLength = 4
    )
    @NotBlank(message = "Export country code is required")
    private String exportCountryCode;

    @Schema(
            description = "Destination Country code (C + 3 numeric characters)",
            example = "C840",
            minLength = 4,
            maxLength = 4
    )
    @NotBlank(message = "Country code is required")
    private String desCountryCode;

    @Schema(
            description = "Customs value for tariff calculation",
            example = "1000.50"
    )
    @Positive(message = "Customs value must be positive")
    private double customsValue;

    @Schema(
            description = "Declaration date (used to determine applicable tariff)",
            example = "2025-01-01"
    )
    private LocalDate date;
}

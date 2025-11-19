package com.lynus.cs203.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response object for tariff calculation")
public class TariffCalculationResponse {
    @Schema(
            description = "Product code for tariff calculation",
            example = "123456"
    )
    private Integer productCode;

    @Schema(
            description = "Export country code",
            example = "CN"
    )
    private String exportCountryCode;

    @Schema(
            description = "Import country code",
            example = "US"
    )
    private String desCountryCode;

    @Schema(
            description = "Customs value for tariff calculation",
            example = "1000.50"
    )
    private double customsValue;

    @Schema(
            description = "Calculated tariff amount",
            example = "150.075"
    )
    private double tariffAmount;

    @Schema(
            description = "Type of trade agreement applied",
            example = "FTA"
    )
    private String agreementType;
}

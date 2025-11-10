package com.lynus.cs203.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response object for tariff rate details")
public class TariffRateResponse {
    @Schema(
            description = "Trade ID for tariff rate details"
    )
    private int trade_id;

    @Schema(
            description = "Reporter ID for tariff rate details"
    )
    private String reporter_code;

    @Schema(
            description = "Reporter Name for tariff rate details"
    )
    private String reporterName;

    @Schema(
            description = "Product code for tariff rate details"
    )
    private String productCode6;

    @Schema(
            description = "hs description for tariff rate details"
    )
    private String hsDescription;

    @Schema(
            description = "hs uom for tariff rate details"
    )
    private String hs_uom;

    @Schema(
            description = "Food category for tariff rate details"
    )
    private String food_category;

    @Schema(
            description = "Tariff rate value"
    )
    private double value;
}

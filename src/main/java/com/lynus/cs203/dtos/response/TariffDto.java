package com.lynus.cs203.dtos.response;

import com.lynus.cs203.entities.Tariff;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response object for tariff details")
public class TariffDto {
    @Schema(
            description = "Unique trade identifier",
            example = "12345"
    )
    public Long trade_id;

    @Schema(
            description = "HS code description",
            example = "Wheat and meslin"
    )
    public String hsDescription;

    @Schema(
            description = "6-digit product code",
            example = "100199"
    )
    public String productCode6;

    @Schema(
            description = "Food category",
            example = "Cereals"
    )
    public String food_category;

    @Schema(
            description = "Tariff rate value",
            example = "12.5"
    )
    public double value;

    @Schema(
            description = "Name of the reporting country",
            example = "United States"
    )
    public String reporterName;

    public static TariffDto fromEntity(Tariff tariff) {
        return TariffDto.builder()
                .trade_id(tariff.getTradeId())
                .hsDescription(tariff.getProduct().getProductDescription())
                .productCode6(tariff.getProduct().getProductCode())
                .food_category(tariff.getProduct().getFoodCategory())
                .value(tariff.getTariffRate())
                .reporterName(tariff.getCountry().getCountryName())
                .build();
    }
}

package com.lynus.cs203.dtos.response;

import com.lynus.cs203.entities.Tariff;
import lombok.Builder;

@Builder
public class TariffDto {
    public Long trade_id;
    public String hsDescription;
    public String productCode6;
    public String food_category;
    public double value;
    public String reporterName;

    public static TariffDto fromEntity(Tariff tariff) {
        return TariffDto.builder()
                .trade_id(tariff.getTradeId())
                .hsDescription(tariff.getProduct().getProductDescription())
                .productCode6(String.valueOf(tariff.getProduct().getProductCode()))
                .food_category(tariff.getProduct().getFoodCategory())
                .value(tariff.getTariffRate())
                .reporterName(tariff.getCountry().getCountryName())
                .build();
    }


}

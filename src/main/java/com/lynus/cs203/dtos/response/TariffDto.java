package com.lynus.cs203.dtos.response;

import com.lynus.cs203.entities.Tariff;

public class TariffDto {
    public Long trade_id;
    public String hsDescription;
    public String productCode6;
    public String food_category;
    public double value;
    public String reporterName;

    public static TariffDto fromEntity(Tariff tariff) {
        TariffDto dto = new TariffDto();
        dto.trade_id = tariff.getTradeId();
        dto.hsDescription = tariff.getProduct().getProductDescription();
        dto.productCode6 = String.valueOf(tariff.getProduct().getProductCode());
        dto.food_category = tariff.getProduct().getFoodCategory();
        dto.value = tariff.getTariffRate();
        dto.reporterName = tariff.getCountry().getCountryName();
        return dto;
    }


}

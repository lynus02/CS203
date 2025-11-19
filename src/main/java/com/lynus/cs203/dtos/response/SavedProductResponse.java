package com.lynus.cs203.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SavedProductResponse {
    private Long id;

    private String configName;
    private double productValue;

    private String originCountry;
    private String destinationCountry;

    private String importDate;
    private String savedAt;

    private ProductDto product;
}
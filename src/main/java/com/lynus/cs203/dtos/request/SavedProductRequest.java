package com.lynus.cs203.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Builder
@Data
@Schema(description = "Request object for saving a product configuration")
public class SavedProductRequest {
    private Long productId;
    private Long originCountryId;
    private Long destinationCountryId;
    private double productValue;
    private String configName;
    private LocalDateTime importDate;
}
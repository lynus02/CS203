package com.lynus.cs203.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDto {
    private Long id;
    private String name;
    private String hsCode;
    private String category;
}

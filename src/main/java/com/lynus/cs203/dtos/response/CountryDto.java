package com.lynus.cs203.dtos.response;

// dto/CountryDto.java
import lombok.Data;
import lombok.Getter;

@Getter
@Data
public class CountryDto {
    private String code;
    private String name;

    public CountryDto() {}

    public CountryDto(String code, String name) {
        this.code = code;
        this.name = name;
    }
}

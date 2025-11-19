package com.lynus.cs203.dtos.response;

// dto/CountryDto.java
import lombok.Data;
import lombok.Getter;

@Getter
@Data
public class CountryDto {
    private Long id;
    private String code;
    private String name;

    public CountryDto() {}

    public CountryDto(Long id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }
}

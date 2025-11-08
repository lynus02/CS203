package com.lynus.cs203.controllers;

import com.lynus.cs203.repositories.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/countries")
@RequiredArgsConstructor
public class CountryController {
    private final CountryRepository countryRepository;

    @GetMapping
    public List<CountryDto> getCountries() {
        return countryRepository.findAll()
                .stream()
                .map(c -> new CountryDto(c.getCountryCode(), c.getCountryName()))
                .collect(Collectors.toList());
    }

    public record CountryDto(String code, String name) {}
}



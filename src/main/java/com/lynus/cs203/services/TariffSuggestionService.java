package com.lynus.cs203.services;

import com.lynus.cs203.controllers.CountryController;
import com.lynus.cs203.dtos.response.CountryDto;
import com.lynus.cs203.dtos.response.TariffDto;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.TariffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TariffSuggestionService {
    @Autowired
    private TariffRepository tariffRepository;

    @Autowired
    private CountryRepository countryRepository;

    public List<TariffDto> getTariffRatesBySize(int size, String countryName) {
        Page<Tariff> tariffs = null;
        if (countryName != null && !countryName.isEmpty()) {
            tariffs = tariffRepository.findByCountry_CountryName(countryName, PageRequest.of(0, size));
        } else {
            tariffs = tariffRepository.findAll(PageRequest.of(0, size));
        }
        return tariffs.map(TariffDto::fromEntity).getContent();
    }

    public Page<TariffDto> suggestProducts(String q, String country, int page, int size) {
        boolean isNumeric = q.matches("\\d+");

        if (country != null && !country.isEmpty()) {
            if (isNumeric) {
                return suggestByCountryAndCodeContaining(country, q, page, size);
            } else {
                return suggestByCountryAndDescription(country, q, page, size);
            }
        } else {
            return suggestByDescriptionOrCodeContaining(q, page, size);
        }
    }

    private Page<TariffDto> suggestByCountryAndCodeContaining(String country, String code, int page, int size) {
        return tariffRepository
                .findByCountry_CountryNameAndProduct_ProductCodeContaining(country, code, PageRequest.of(page, size))
                .map(TariffDto::fromEntity);
    }


    private Page<TariffDto> suggestByCountryAndDescription(String country, String desc, int page, int size) {
        return tariffRepository
                .findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(country, desc, PageRequest.of(page, size))
                .map(TariffDto::fromEntity);
    }


    private Page<TariffDto> suggestByDescriptionOrCodeContaining(String desc, int page, int size) {
        return tariffRepository
                .findByProductDescriptionOrProductCodeContaining(desc, desc, PageRequest.of(page, size))
                .map(TariffDto::fromEntity);
    }


    public List<CountryDto> getAllCountries() {
        return countryRepository.findAll().stream()
                .map(c -> new CountryDto(c.getCountryCode(), c.getCountryName()))
                .toList();
    }

}
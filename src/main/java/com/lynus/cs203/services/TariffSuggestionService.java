package com.lynus.cs203.services;

import com.lynus.cs203.dtos.response.TariffDto;
import com.lynus.cs203.entities.Tariff;
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

    public List<TariffDto> getTariffRatesBySize(int size, String country) {
        Page<Tariff> tariffs = tariffRepository.findByCountry_CountryName(country, PageRequest.of(0, size));
        return tariffs.map(TariffDto::fromEntity).getContent();
    }

    public Page<TariffDto> suggestProducts(String q, String country, int page, int size) {
        boolean isNumeric = q.matches("\\d+");
        if (country != null && !country.isEmpty()) {
            if (isNumeric) {
                return suggestByCountryAndCodeContaining(country, Integer.valueOf(q), page, size);
            } else {
                return suggestByCountryAndDescription(country, q, page, size);
            }
        } else {
            return suggestByDescriptionOrCodeContaining(q, isNumeric ? Integer.valueOf(q) : -1, page, size);
        }
    }

    private Page<TariffDto> suggestByCountryAndCodeContaining(String country, int code, int page, int size) {
        return tariffRepository
                .findByCountry_CountryNameAndProduct_ProductCode(country, code, PageRequest.of(page, size))
                .map(TariffDto::fromEntity);
    }

    private Page<TariffDto> suggestByCountryAndDescription(String country, String desc, int page, int size) {
        return tariffRepository
                .findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(country, desc, PageRequest.of(page, size))
                .map(TariffDto::fromEntity);
    }

    private Page<TariffDto> suggestByDescriptionOrCodeContaining(String desc, int code, int page, int size) {
        return tariffRepository
                .findByProductDescriptionOrProductCodeContaining(desc.toLowerCase(), code, PageRequest.of(page, size))
                .map(TariffDto::fromEntity);
    }

}
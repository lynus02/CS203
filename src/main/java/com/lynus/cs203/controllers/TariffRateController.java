package com.lynus.cs203.controllers;

import com.lynus.cs203.entities.TariffRate;
import com.lynus.cs203.repositories.TariffRateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tariff-rates")
@CrossOrigin // Allow frontend to access this endpoint
public class TariffRateController {

    private final TariffRateRepository tariffRateRepository;

    public TariffRateController(TariffRateRepository tariffRateRepository) {
        this.tariffRateRepository = tariffRateRepository;
    }

    @GetMapping
    public List<TariffRate> getTariffRates(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String country) {
        int cappedSize = Math.min(size, 25);
        if (country != null && !country.isEmpty()) {
            return tariffRateRepository
                    .findByReporterNameContainingIgnoreCase(country, PageRequest.of(0, cappedSize))
                    .getContent();
        }
        return tariffRateRepository.findAll(PageRequest.of(0, cappedSize)).getContent();
    }


    @GetMapping("/hs-descriptions")
    public Page<TariffRate> getHsDescriptions(
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String productCode6,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return tariffRateRepository.findByHsDescriptionContainingIgnoreCaseAndProductCode6ContainingIgnoreCaseAndReporterNameContainingIgnoreCase(
                description == null ? "" : description,
                productCode6 == null ? "" : productCode6,
                country == null ? "" : country,
                PageRequest.of(page, size));
    }

    @GetMapping("/suggest")
    public Page<TariffRate> suggestProducts(@RequestParam("q") String query,
                                            @RequestParam(required = false) String country,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        if (country != null && !country.isEmpty()) {
            // Search by description and code, both must match country
            Page<TariffRate> byDesc = tariffRateRepository
                    .findByReporterNameContainingIgnoreCaseAndHsDescriptionContainingIgnoreCase(
                            country, query, PageRequest.of(page, size));
            Page<TariffRate> byCode = tariffRateRepository
                    .findByReporterNameContainingIgnoreCaseAndProductCode6ContainingIgnoreCase(
                            country, query, PageRequest.of(page, size));
            // Combine results (if needed, or just return one)
            // Here, you can merge the two pages' contents if you want both
            // For simplicity, return byDesc if query is not numeric, byCode if numeric
            if (query.matches("\\d+")) {
                return byCode;
            }
            return byDesc;
        }
        return tariffRateRepository.findByHsDescriptionContainingIgnoreCaseOrProductCode6ContainingIgnoreCase(
                query, query, PageRequest.of(page, size));
    }


}
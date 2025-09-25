package com.lynus.cs203.controllers;

import com.lynus.cs203.entities.TariffRate;
import com.lynus.cs203.repositories.TariffRateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tariff-rates")
@CrossOrigin // Allow frontend to access this endpoint
public class TariffRateController {

    private final TariffRateRepository tariffRateRepository;

    public TariffRateController(TariffRateRepository tariffRateRepository) {
        this.tariffRateRepository = tariffRateRepository;
    }

    @GetMapping
    public List<TariffRate> getAllTariffRates() {
        return tariffRateRepository.findAll();
    }

    @GetMapping("/hs-descriptions")
    public Page<TariffRate> getHsDescriptions(
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return tariffRateRepository.findByHsDescriptionContainingIgnoreCaseOrProductCode6ContainingIgnoreCase(description, country, PageRequest.of(page, size));
    }

    @GetMapping("/suggest")
    public Page<TariffRate> suggestProducts(@RequestParam("q") String query, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return tariffRateRepository.findByHsDescriptionContainingIgnoreCaseOrProductCode6ContainingIgnoreCase(query, query, PageRequest.of(page, size));
    }

}
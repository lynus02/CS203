package com.lynus.cs203.controllers;

import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.ProductRepository;
import com.lynus.cs203.services.TariffCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TariffController {
    private final TariffCalculationService tariffCalculationService;
    public TariffController(TariffCalculationService tariffCalculationService) {
        this.tariffCalculationService = tariffCalculationService;
    }

    @GetMapping("/tariff-form")
    public String showTariffForm() {
        // return the view name (without ".html")
        return "tariff_form";
    }

    @PostMapping("/calculate-tariff")
    public Map<String, Object> calculate(@RequestParam Integer productCode,
                                         @RequestParam String countryCode,
                                         @RequestParam double customsValue) {
        double baseTariffRate = tariffCalculationService.getTariffRate(productCode, countryCode);
        double tariffAmount = tariffCalculationService.calculateTariff(productCode, countryCode, customsValue);
        double totalCost = customsValue + tariffAmount;

        Map<String, Object> response = new HashMap<>();
        response.put("product", productCode);
        response.put("country", countryCode);
        response.put("customsValue", customsValue);
        response.put("baseTariffRate", baseTariffRate);
        response.put("dutyAmount", tariffAmount);
        response.put("totalCost", totalCost);
        return response;

    }



}
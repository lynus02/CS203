package com.lynus.cs203.controllers;

import com.lynus.cs203.services.TariffCalculationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
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
        double tariffAmount = tariffCalculationService.calculateTariff(productCode, countryCode, customsValue);

        Map<String, Object> response = new HashMap<>();
        response.put("product", productCode);
        response.put("country", countryCode);
        response.put("customsValue", customsValue);
        response.put("tariffAmount", tariffAmount);

        return response;

    }


}
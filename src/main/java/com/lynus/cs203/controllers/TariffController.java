package com.lynus.cs203.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TariffController {

    @GetMapping("/tariff-form")
    public String showTariffForm() {
        // return the view name (without ".html")
        return "tariff_form";
    }

    @PostMapping("/calculate-tariff")
    public String calculateTariff(
            @RequestParam("hsDescription") String hsDescription,
            @RequestParam("hsCode") String hsCode,
            @RequestParam("cifValue") double cifValue,
            @RequestParam("tariffRate") double tariffRate,
            @RequestParam("vatRate") double vatRate,
            Model model){

        // tariff calculation // to be done by zhao yun


        return "tariff_result"; // html to show result of calculated tariff
    }


}
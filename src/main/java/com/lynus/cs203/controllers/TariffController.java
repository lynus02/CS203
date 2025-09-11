package com.lynus.cs203.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TariffController {

    @GetMapping("/tariff-form")
    public String showTariffForm() {
        // return the view name (without ".html")
        return "tariff_form";
    }
}
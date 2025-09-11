package com.lynus.cs203.controllers;

import com.lynus.cs203.entities.HSCode;
import com.lynus.cs203.repositories.HSCodeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SuggestionController {

    private final HSCodeRepository hsCodeRepository;
    private final List<String> hsDescription;
    private final List<String> hsCodes;

    public SuggestionController(HSCodeRepository hsCodeRepository) {
        this.hsCodeRepository = hsCodeRepository;
        this.hsDescription = hsCodeRepository.findAll().stream()
                .map(HSCode::getDescription)
                .toList();
        this.hsCodes = hsCodeRepository.findAll().stream()
                .map(HSCode::getHsCode)
                .toList();
    }

    @GetMapping("/search")
    public List<HSCode> search(@RequestParam String query) {
        return hsCodeRepository.findByDescriptionContainingIgnoreCaseOrHsCodeContainingIgnoreCase(query, query);
    }

    @GetMapping("/hsDescriptions")
    public List<String> suggestDescription(@RequestParam String query) {
        return hsDescription.stream()
                .filter(desc -> desc.toLowerCase().contains(query.toLowerCase()))
                .toList();
    }

    @GetMapping("/hscodes")
    public List<String> suggestHsCodes(@RequestParam String query) {
        return hsCodes.stream()
                .filter(code -> code.toLowerCase().contains(query.toLowerCase()))
                .toList();
    }

    @GetMapping("/hsCodeByDescription")
    public String getHsCodeByDescription(@RequestParam String description) {
        return hsCodeRepository.findByDescription(description)
                .map(HSCode::getHsCode)
                .orElse("");
    }

    @GetMapping("/hsDescriptionByCode")
    public String getHsDescriptionByCode(@RequestParam String hsCode) {
        return hsCodeRepository.findByHsCode(hsCode)
                .map(HSCode::getDescription)
                .orElse("");
    }


}
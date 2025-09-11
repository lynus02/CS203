package com.lynus.cs203.controllers;

import com.lynus.cs203.entities.HSCode;
import com.lynus.cs203.repositories.HSCodeRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<HSCode>> search(@RequestParam String query) {
        List<HSCode> result = hsCodeRepository.findByDescriptionContainingIgnoreCaseOrHsCodeContainingIgnoreCase(query, query);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/hsDescriptions")
    public ResponseEntity<List<String>> suggestDescription(@RequestParam String query) {
        List<String> result = hsDescription.stream()
                .filter(desc -> desc.toLowerCase().contains(query.toLowerCase()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/hscodes")
    public ResponseEntity<List<String>> suggestHsCodes(@RequestParam String query) {
        List<String> result = hsCodes.stream()
                .filter(code -> code.toLowerCase().contains(query.toLowerCase()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/hsCodeByDescription")
    public ResponseEntity<String> getHsCodeByDescription(@RequestParam String description) {
        return hsCodeRepository.findByDescription(description)
                .map(HSCode -> ResponseEntity.ok(HSCode.getHsCode()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/hsDescriptionByCode")
    public ResponseEntity<String> getHsDescriptionByCode(@RequestParam String hsCode) {
        return hsCodeRepository.findByHsCode(hsCode)
                .map(HSCode -> ResponseEntity.ok(HSCode.getDescription()))
                .orElse(ResponseEntity.notFound().build());
    }


}
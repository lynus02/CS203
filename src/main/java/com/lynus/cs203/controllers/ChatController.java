package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.ChatRequest;
import com.lynus.cs203.services.OpenAIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatController {

    private final OpenAIService openAIService;

    public ChatController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatRequest request) {
        String userPrompt = request.getPrompt();
        if (userPrompt == null || userPrompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt cannot be null or empty"));
        }

        try {
            Map<String, String> response = openAIService.processChat(userPrompt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

package com.lynus.cs203.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lynus.cs203.dtos.request.ChatRequest;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.TariffRepository;
import com.lynus.cs203.repositories.TradeAgreementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/chatbot")
public class ChatController {

    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";

    @Autowired
    private TariffRepository tariffRepo;

    @Autowired
    private TradeAgreementRepository tradeRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatRequest request) {
        String userPrompt = request.getPrompt();

        if (userPrompt == null || userPrompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt cannot be null or empty"));
        }

        try {
            RestTemplate rest = new RestTemplate();

            Map<String, Object> extractBody = Map.of(
                    "model", "llama3",
                    "stream", false,
                    "messages", List.of(
                            Map.of("role", "system",
                                    "content", "Extract the product, origin country, and destination country from the user's question. Respond ONLY in JSON like {\"product\":\"...\",\"origin\":\"...\",\"destination\":\"...\"}."),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            ResponseEntity<String> extractResponse = rest.postForEntity(OLLAMA_URL, extractBody, String.class);
            System.out.println("Ollama Extract Response:\n" + extractResponse.getBody());

            JsonNode parsed = mapper.readTree(extractResponse.getBody());
            String content = parsed.path("message").path("content").asText("{}");

            JsonNode extraction;
            try {
                extraction = mapper.readTree(content);
            } catch (Exception ex) {
                System.out.println("Could not parse extraction as JSON: " + content);
                extraction = mapper.createObjectNode();
            }

            String product = extraction.path("product").asText("");
            String origin = extraction.path("origin").asText("");
            String destination = extraction.path("destination").asText("");

            List<Tariff> tariffs = tariffRepo
                    .findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(destination, product, Pageable.unpaged())
                    .getContent();

            String tariffSummary = tariffs.isEmpty()
                    ? "No tariff records found for this query."
                    : tariffs.stream()
                    .map(t -> String.format(
                            "HS %s (%s): %.2f%% tariff for %s",
                            t.getProduct().getProductCode(),
                            t.getProduct().getProductDescription(),
                            t.getTariffRate(),
                            t.getCountry().getCountryName()))
                    .reduce("", (a, b) -> a + "\n" + b);

            String context = String.format(
                    "Database info:\nProduct: %s\nOrigin: %s\nDestination: %s\n\nTariff data:\n%s\n\nQuestion: %s",
                    product, origin, destination, tariffSummary, userPrompt);

            Map<String, Object> answerBody = Map.of(
                    "model", "llama3",
                    "stream", false,
                    "messages", List.of(
                            Map.of("role", "system",
                                    "content", "You are a trade assistant. Use the data provided to answer factually and concisely."),
                            Map.of("role", "user", "content", context)
                    )
            );

            ResponseEntity<String> answerResponse = rest.postForEntity(OLLAMA_URL, answerBody, String.class);
            System.out.println("Ollama Answer Response:\n" + answerResponse.getBody());

            JsonNode answerNode = mapper.readTree(answerResponse.getBody());
            String answer = answerNode.path("message").path("content").asText("No response from model.");

            return ResponseEntity.ok(Map.of(
                    "answer", answer,
                    "product", product,
                    "origin", origin,
                    "destination", destination
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

package com.lynus.cs203.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lynus.cs203.dtos.request.ChatRequest;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.TariffRepository;
import com.lynus.cs203.repositories.TradeAgreementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

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

            // extract info from ollama
            Map<String, Object> extractBody = Map.of(
                    "model", "llama3",
                    "stream", false,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", """
                            Extract ONLY the main product keyword from the user’s question.
                            Do NOT include origin, destination, country names, or tariff rates unless prompted so.
                            Respond strictly in JSON like {"product": "pork"}.
                            If there are multiple foods, pick the most relevant one.
                            Example:
                            Input: "What is the HS code for ham from Spain?"
                            Output: {"product": "ham"}
                            """
                            ),
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
            product = product.replaceAll("(?i)origin|destination|country", "").trim();
            String origin = extraction.path("origin").asText("");
            String destination = extraction.path("destination").asText("");

            // search from db first before asking ollama
            Pageable limit = PageRequest.of(0, 20); // limit results
            List<Tariff> tariffs;

            if (!destination.isBlank() && !product.isBlank()) {
                tariffs = tariffRepo
                        .findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(destination, product, limit)
                        .getContent();
            } else if (!product.isBlank()) {
                tariffs = tariffRepo
                        .findByProduct_ProductDescriptionContainingIgnoreCaseOrProduct_ProductCode(product, safeParseInt(product), limit)
                        .getContent();
            } else {
                tariffs = tariffRepo.findAll(limit).getContent();
            }

            // fallback if no results
            if (tariffs.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "answer", "No relevant tariff records found. Please try rephrasing your question.",
                        "product", product,
                        "origin", origin,
                        "destination", destination
                ));
            }

            // rank top N results by relevance
            String lowerProduct = product.toLowerCase();
            List<Tariff> relevantTariffs = tariffs.stream()
                    .sorted(Comparator.comparingDouble(
                            t -> -similarityScore(t.getProduct().getProductDescription(), lowerProduct)))
                    .limit(10)
                    .collect(Collectors.toList());

            // format context for ollama
            String tariffSummary = relevantTariffs.stream()
                    .map(t -> String.format(
                            "HS %s (%s): %.2f%% tariff for %s",
                            t.getProduct().getProductCode(),
                            t.getProduct().getProductDescription(),
                            t.getTariffRate(),
                            t.getCountry().getCountryName()))
                    .collect(Collectors.joining("\n"));

            String context = String.format("""
                    Query:
                    %s

                    Database matches:
                    %s
                    """, userPrompt, tariffSummary);

            // ask ollama for best match
            Map<String, Object> answerBody = Map.of(
                    "model", "llama3",
                    "stream", false,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", """
                            You are a customs trade assistant.
                            Use ONLY the following database matches.
                            If several products look similar, ask the user to clarify
                            (e.g. "Do you mean raw ham or cooked ham?").
                            Never invent data. Always cite the HS code in your answer.
                            """
                            ),
                            Map.of("role", "user", "content", context)
                    )
            );


            ResponseEntity<String> answerResponse = rest.postForEntity(OLLAMA_URL, answerBody, String.class);
            System.out.println("Ollama Answer Response:\n" + answerResponse.getBody());

            JsonNode answerNode = mapper.readTree(answerResponse.getBody());
            String answer = answerNode.path("message").path("content").asText("No response from model.");

            // return response
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

   // util methods
    private static int safeParseInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D", "")); // extract digits only
        } catch (Exception e) {
            return 0;
        }
    }

    private static double similarityScore(String text1, String text2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));
        if (words1.isEmpty() || words2.isEmpty()) return 0;
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        return (double) intersection.size() / Math.max(words1.size(), words2.size());
    }
}
package com.lynus.cs203.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.TariffRepository;
import com.lynus.cs203.repositories.AgreementCountryRepository;
import com.lynus.cs203.repositories.TradeAgreementRepository;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OpenAIService {

    private final TariffRepository tariffRepo;
    private final TradeAgreementRepository tradeRepo;
    private final AgreementCountryRepository agreementCountryRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String apiKey;

    public OpenAIService(TariffRepository tariffRepo, TradeAgreementRepository tradeRepo, AgreementCountryRepository agreementCountryRepo) {
        this.tariffRepo = tariffRepo;
        this.tradeRepo = tradeRepo;
        this.agreementCountryRepo = agreementCountryRepo;
    }

    public Map<String, String> processChat(String userPrompt) throws Exception {
        OpenAiService service = new OpenAiService(apiKey);

        // Step 1: Classify the intent
        String classifyPrompt = """
            Classify the user's question into one of these categories:
            ["HS_CODE", "TRADE_AGREEMENT", "OTHER"].
            Respond strictly in JSON: {"intent": "<category>"}.
            Examples:
            - "What is the HS code for pork?" → {"intent": "HS_CODE"}
            - "Is there a free trade agreement between Japan and Singapore?" → {"intent": "TRADE_AGREEMENT"}
        """;

        ChatCompletionRequest classifyReq = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(
                        new ChatMessage("system", classifyPrompt),
                        new ChatMessage("user", userPrompt)
                ))
                .temperature(0.0)
                .build();

        String classifyJson = service.createChatCompletion(classifyReq)
                .getChoices().get(0).getMessage().getContent();
        JsonNode classifyNode = mapper.readTree(classifyJson);
        String intent = classifyNode.path("intent").asText("OTHER");

        switch (intent) {
            case "HS_CODE":
                return handleHSCodeQuery(service, userPrompt);
            case "TRADE_AGREEMENT":
                return handleTradeAgreementQuery(service, userPrompt);
            default:
                return Map.of("answer", "I can help with HS codes or trade agreements — could you clarify what you’d like to know?");
        }
    }

    // -----------------------------------------------
    // HS CODE QUERIES
    // -----------------------------------------------
    private Map<String, String> handleHSCodeQuery(OpenAiService service, String userPrompt) throws Exception {
        String extractPrompt = """
            Extract ONLY the main product keyword from the question.
            Return JSON: {"product": "<word>"}.
        """;

        ChatCompletionRequest extractReq = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(
                        new ChatMessage("system", extractPrompt),
                        new ChatMessage("user", userPrompt)
                ))
                .temperature(0.0)
                .build();

        String extractJson = service.createChatCompletion(extractReq)
                .getChoices().get(0).getMessage().getContent();

        JsonNode extraction = mapper.readTree(extractJson);
        String product = extraction.path("product").asText("").trim();

        if (product.isBlank()) {
            return Map.of("answer", "Could you rephrase that? I couldn’t detect a product name.");
        }

        var limit = PageRequest.of(0, 10);
        List<Tariff> tariffs = tariffRepo
                .findByProductDescriptionOrProductCodeContaining(product, product, limit)
                .getContent();

        if (tariffs.isEmpty()) {
            String fallbackPrompt = String.format("""
                The product is "%s".
                The database has no match.
                Based on Harmonized System classification knowledge, give one likely HS code and short reason.
                Respond in plain text.
            """, product);

            ChatCompletionRequest req = ChatCompletionRequest.builder()
                    .model("gpt-4o-mini")
                    .messages(List.of(
                            new ChatMessage("system", "You are an HS code expert."),
                            new ChatMessage("user", fallbackPrompt)
                    ))
                    .temperature(0.2)
                    .build();

            String gptAnswer = service.createChatCompletion(req)
                    .getChoices().get(0).getMessage().getContent();

            return Map.of("answer", gptAnswer, "product", product);
        }

        String summary = tariffs.stream()
                .map(t -> String.format(
                        "HS %s — %s (%.2f%% tariff in %s)",
                        t.getProduct().getProductCode(),
                        t.getProduct().getProductDescription(),
                        t.getTariffRate(),
                        t.getCountry().getCountryName()
                ))
                .collect(Collectors.joining("\n"));

        String answerPrompt = String.format("""
            You are an HS code assistant.
            The user asked: "%s"
            Database records:
            %s

            Summarize in 1–3 sentences which HS code best matches.
        """, userPrompt, summary);

        ChatCompletionRequest answerReq = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(
                        new ChatMessage("system", "You are a factual customs assistant."),
                        new ChatMessage("user", answerPrompt)
                ))
                .temperature(0.2)
                .build();

        String answer = service.createChatCompletion(answerReq)
                .getChoices().get(0).getMessage().getContent();

        return Map.of("answer", answer, "product", product);
    }

    // -----------------------------------------------
    // TRADE AGREEMENT QUERIES
    // -----------------------------------------------
    private Map<String, String> handleTradeAgreementQuery(OpenAiService service, String userPrompt) throws Exception {
        String extractPrompt = """
        Extract the two countries being compared in the question.
        Return JSON: {"country1": "<name>", "country2": "<name>"}.
    """;

        ChatCompletionRequest extractReq = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(
                        new ChatMessage("system", extractPrompt),
                        new ChatMessage("user", userPrompt)
                ))
                .temperature(0.0)
                .build();

        String extractJson = service.createChatCompletion(extractReq)
                .getChoices().get(0).getMessage().getContent();

        JsonNode extraction = mapper.readTree(extractJson);
        String country1 = extraction.path("country1").asText("").trim();
        String country2 = extraction.path("country2").asText("").trim();

        country1 = normalizeCountryName(country1);
        country2 = normalizeCountryName(country2);

        if (country1.isBlank() || country2.isBlank()) {
            return Map.of("answer", "Could you specify both countries?");
        }

        // Step 1: Find all shared trade agreements
        List<Long> agreementIds = agreementCountryRepo.findAgreementsBetweenCountries(country1, country2);

        if (agreementIds.isEmpty()) {
            return Map.of("answer", String.format("No trade agreement found between %s and %s.", country1, country2));
        }

        // Step 2: Fetch agreement details (Name + Type)
        List<String> agreements = tradeRepo.findAllByIds(agreementIds).stream()
                .map(a -> String.format("%s (%s)", a.getAgreementName(), a.getAgreementType()))
                .toList();


        // Step 3: Build a friendly summary
        String joinedAgreements = String.join("; ", agreements);
        String answer = String.format(
                "Yes, there is at least one trade agreement between %s and %s: %s.",
                country1, country2, joinedAgreements
        );

        return Map.of(
                "answer", answer,
                "country1", country1,
                "country2", country2,
                "agreements", joinedAgreements
        );
    }

    // helper methods
    private String normalizeCountryName(String input) {
        Map<String, String> aliases = Map.ofEntries(
                Map.entry("usa", "united states of america"),
                Map.entry("us", "united states of america"),
                Map.entry("u.s.", "united states of america"),
                Map.entry("uk", "united kingdom"),
                Map.entry("uae", "united arab emirates"),
                Map.entry("south korea", "republic of korea"),
                Map.entry("north korea", "democratic people's republic of korea"),
                Map.entry("vietnam", "viet nam")
        );
        String lower = input.toLowerCase().trim();
        return aliases.getOrDefault(lower, lower);
    }

}

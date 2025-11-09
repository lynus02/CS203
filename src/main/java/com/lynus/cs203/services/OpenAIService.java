package com.lynus.cs203.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.TariffRepository;
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
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String apiKey;

    public OpenAIService(TariffRepository tariffRepo) {
        this.tariffRepo = tariffRepo;
    }

//    private String detectIntent(OpenAiService service, String prompt) {
//        String classifyPrompt = """
//        Classify the user's request into one of these categories:
//        ["HS_CODE", "TRADE_AGREEMENT", "TARIFF_RATE", "OTHER"].
//        Respond strictly in JSON: {"intent": "<category>"}.
//        Example:
//        - "What is the HS code for pork?" → {"intent": "HS_CODE"}
//        - "Is there a free trade agreement between Japan and Singapore?" → {"intent": "TRADE_AGREEMENT"}
//    """;
//
//        ChatCompletionRequest classifyReq = ChatCompletionRequest.builder()
//                .model("gpt-4o-mini")
//                .messages(List.of(
//                        new ChatMessage("system", classifyPrompt),
//                        new ChatMessage("user", prompt)
//                ))
//                .temperature(0.0)
//                .build();
//
//        try {
//            String content = service.createChatCompletion(classifyReq)
//                    .getChoices().get(0).getMessage().getContent();
//            JsonNode node = new ObjectMapper().readTree(content);
//            return node.path("intent").asText("OTHER");
//        } catch (Exception e) {
//            return "OTHER";
//        }
//    }

    public Map<String, String> processChat(String userPrompt) throws Exception {
        OpenAiService service = new OpenAiService(apiKey);
        ObjectMapper mapper = new ObjectMapper();

        // extract product keyword
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

        // query db
        var limit = PageRequest.of(0, 10);
        List<Tariff> tariffs = tariffRepo
                .findByProduct_ProductDescriptionContainingIgnoreCaseOrProduct_ProductCode(product, safeParseInt(product), limit)
                .getContent();

        // fallback if no result
        if (tariffs.isEmpty()) {
            String fallbackPrompt = String.format("""
            The product is "%s".
            The database has no match.
            Based on Harmonized System classification knowledge, give one likely HS code and short reason.
            Respond in plain text, without apology or unrelated examples.
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

            return Map.of(
                    "answer", gptAnswer,
                    "product", product
            );
        }

        // summarize results
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

        Summarize in 1–3 sentences which HS code best matches the query.
        If multiple are similar, name the top one and mention others briefly.
        Do not invent data.
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

        return Map.of(
                "answer", answer,
                "product", product
        );
    }

    // util methods
    private String getSynonym(OpenAiService service, String product) {
        try {
            String prompt = String.format("""
                Suggest a more general or related food keyword (single word)
                that customs databases might use for "%s".
                Respond only with one word.
            """, product);

            ChatCompletionRequest synonymReq = ChatCompletionRequest.builder()
                    .model("gpt-4o-mini")
                    .messages(List.of(
                            new ChatMessage("system", "You are helping map food names to standardized HS terms."),
                            new ChatMessage("user", prompt)
                    ))
                    .temperature(0.3)
                    .build();

            ChatCompletionResult synonymRes = service.createChatCompletion(synonymReq);
            return synonymRes.getChoices().get(0).getMessage().getContent().trim();
        } catch (Exception e) {
            return product;
        }
    }

    private static int safeParseInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D", ""));
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
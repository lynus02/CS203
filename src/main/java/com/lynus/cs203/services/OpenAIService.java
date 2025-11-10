package com.lynus.cs203.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.TariffRepository;
import com.lynus.cs203.repositories.AgreementCountryRepository;
import com.lynus.cs203.repositories.TradeAgreementRepository;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
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

    @Value("${open.api.key}")
    private String apiKey;

    public OpenAIService(TariffRepository tariffRepo, TradeAgreementRepository tradeRepo, AgreementCountryRepository agreementCountryRepo) {
        this.tariffRepo = tariffRepo;
        this.tradeRepo = tradeRepo;
        this.agreementCountryRepo = agreementCountryRepo;
    }

    public Map<String, String> processChat(String userPrompt) throws Exception {
        OpenAiService service = new OpenAiService(apiKey);

        // ask openai to categorize user input: hscode/trade agreement/other
        String classifyPrompt = """
            Classify the user's question into one of these categories:
            ["HS_CODE", "TRADE_AGREEMENT", "TARIFF_RATE", "OTHER"].
            Respond strictly in JSON: {"intent": "<category>"}.
            Examples:
            - "What is the HS code for pork?" → {"intent": "HS_CODE"}
            - "Is there a free trade agreement between Japan and Singapore?" → {"intent": "TRADE_AGREEMENT"}
            - "What is the tariff rate for rice imported to Japan?" → {"intent": "TARIFF_RATE"}
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
        String intent = classifyNode.path("intent").asText("OTHER").trim().toUpperCase();

        switch (intent) {
            case "HS_CODE":
                return handleHSCodeQuery(service, userPrompt);
            case "TRADE_AGREEMENT":
                return handleTradeAgreementQuery(service, userPrompt);
            case "TARIFF_RATE":
                return handleTariffRateQuery(service, userPrompt);
            default:
                return Map.of("answer", "I can help with HS codes, trade agreements, or tariff rates — could you clarify what you’d like to know?");
        }
    }

    // -----------------------------------------------
    // HS CODE QUERIES
    // -----------------------------------------------
    private Map<String, String> handleHSCodeQuery(OpenAiService service, String userPrompt) throws Exception {
        // ask openai to extract product keyword
        String extractPrompt = """
            Extract ONLY the main product keyword from the question.
            Return JSON: {"product": "<word>"}.
        """;

        String extractJson = ask(service, extractPrompt, userPrompt);
        JsonNode extraction = mapper.readTree(extractJson);
        String product = extraction.path("product").asText("").trim();

        if (product.isBlank()) {
            return Map.of("answer", "Could you rephrase that? I couldn’t detect a product name.");
        }

        // search db for matching products
        var limit = PageRequest.of(0, 10);
        List<Tariff> tariffs = tariffRepo
                .findByProductDescriptionOrProductCodeContaining(product, product, limit)
                .getContent(); // executes sql query from TariffRepository, limit to 10 results only

        // if no matches, fallback to gpt reasoning to give likely HS code
        if (tariffs.isEmpty()) {
            String fallbackPrompt = String.format("""
                The product is "%s".
                The database has no match.
                Based on Harmonized System classification knowledge, give one likely HS code and short reason.
                Respond in plain text.
            """, product);

            String gptAnswer = ask(service, "You are an HS code expert.", fallbackPrompt, 0.2);
            return Map.of("answer", gptAnswer, "product", product);
        }

        // summarize matching tariffs
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

        // build chat completion request (message to gpt)
        String answer = ask(service, "You are a factual customs assistant.", answerPrompt, 0.2);
        return Map.of("answer", answer, "product", product);
    }

    // -----------------------------------------------
    // TRADE AGREEMENT QUERIES
    // -----------------------------------------------
    private Map<String, String> handleTradeAgreementQuery(OpenAiService service, String userPrompt) throws Exception {
        // ask openai to extract the two countries being compared
        String extractPrompt = """
        Extract the two countries being compared in the question.
        Return JSON: {"country1": "<name>", "country2": "<name>"}.
    """;

        // get ChatCompletionResult object (more than one) --> return the first choice --> extract the message --> extract the content to JSON string
        String extractJson = ask(service, extractPrompt, userPrompt);
        JsonNode extraction = mapper.readTree(extractJson); // behaves like a mini JSON document in mem

        // extract country names from tree and return missing node instead of crashing if not found --> convert to string
        String country1 = normalizeCountryName(extraction.path("country1").asText("").trim());
        String country2 = normalizeCountryName(extraction.path("country2").asText("").trim());
        // ^^ normalize country names

        if (country1.isBlank() || country2.isBlank()) {
            return Map.of("answer", "Could you specify both countries?");
        }

        // find all list of matching trade agreements from db
        List<Long> agreementIds = agreementCountryRepo.findAgreementsBetweenCountries(country1, country2);

        if (agreementIds.isEmpty()) {
            return Map.of("answer", String.format("No trade agreement found between %s and %s.", country1, country2));
        }

        // fetch agreement details (Name + Type)
        List<String> agreements = tradeRepo.findAllByIds(agreementIds).stream()
                .map(a -> String.format("%s (%s)", a.getAgreementName(), a.getAgreementType()))
                .toList();

        // summarize results
        String joinedAgreements = String.join("; ", agreements);
        String answer = String.format(
                "Yes, there is at least one trade agreement between %s and %s: %s.",
                country1, country2, joinedAgreements
        );

        // if user also asked about tariff rates, combine results w tariff calculation
        if (userPrompt.toLowerCase().contains("tariff")) {
            String product = extractProductFromPrompt(service, userPrompt);
            if (!product.isBlank()) {
                return calculateEffectiveTariff(product, country1, country2);
            }
        }

        return Map.of("answer", answer, "agreements", joinedAgreements);
    }

    // -----------------------------------------------
    // TARIFF RATE QUERIES
    // -----------------------------------------------
    private Map<String, String> handleTariffRateQuery(OpenAiService service, String userPrompt) throws Exception {
        // extract relevant product and destination country
        String extractPrompt = """
        Extract the product and destination country mentioned in the question.
        Return JSON: {"product": "<word>", "destination": "<country>"}.
        If the country is not specified, leave it blank.
        """;

        String extractJson = ask(service, extractPrompt, userPrompt);
        JsonNode extraction = mapper.readTree(extractJson);
        String product = extraction.path("product").asText("").trim();
        String destination = extraction.path("destination").asText("").trim();

        destination = normalizeCountryName(destination);

        if (product.isBlank()) {
            return Map.of("answer", "Could you rephrase that? I couldn’t detect a product name.");
        }

        if (destination.isBlank()) {
            destination = "singapore"; // default fallback if user omits country
        }

        // search db for matching tariffs on destination + product
        var limit = PageRequest.of(0, 10);
        List<Tariff> tariffs = tariffRepo
                .findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(destination, product, limit)
                .getContent();

        // fallback if no matches found
        if (tariffs.isEmpty()) {
            String fallbackPrompt = String.format("""
            The product is "%s".
            The database has no match for tariffs to %s.
            Based on global customs data, estimate a likely tariff range (in percent)
            and explain the reasoning briefly (1–2 sentences).
        """, product, destination);

            String gptAnswer = ask(service, "You are a customs tariff expert.", fallbackPrompt, 0.2);
            return Map.of("answer", gptAnswer);
        }

        // summarize result
        double avgTariff = tariffs.stream()
                .mapToDouble(Tariff::getTariffRate)
                .average()
                .orElse(0);

        String summary = tariffs.stream()
                .map(t -> String.format("HS %s — %.2f%% tariff", t.getProduct().getProductCode(), t.getTariffRate()))
                .collect(Collectors.joining(", "));

        String answer = String.format(
                "The average tariff rate for %s imported to %s is approximately %.2f%% based on available records (%s).",
                product, destination, avgTariff, summary);

        return Map.of("answer", answer);
    }

    // -----------------------------------------------
    // TARIFF RATE CALCULATION WITH AGREEMENTS
    // -----------------------------------------------
    private Map<String, String> calculateEffectiveTariff(String product, String originCountry, String destinationCountry) {
        var limit = PageRequest.of(0, 5);
        List<Tariff> tariffs = tariffRepo
                .findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(destinationCountry, product, limit)
                .getContent();

        if (tariffs.isEmpty()) {
            return Map.of("answer",
                    String.format("No tariff data found for %s imported to %s.", product, destinationCountry));
        }

        double avgTariff = tariffs.stream()
                .mapToDouble(Tariff::getTariffRate)
                .average()
                .orElse(0);

        List<Long> agreements = agreementCountryRepo.findAgreementsBetweenCountries(originCountry, destinationCountry);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "For %s imported to %s (HS %s), the base tariff is around %.2f%%.",
                product,
                destinationCountry,
                tariffs.get(0).getProduct().getProductCode(),
                avgTariff
        ));

        if (!agreements.isEmpty()) {
            List<String> agreementNames = tradeRepo.findAllByIds(agreements).stream()
                    .map(a -> String.format("%s (%s)", a.getAgreementName(), a.getAgreementType()))
                    .toList();
            sb.append(String.format(
                    " However, since %s and %s are covered by %s, preferential or zero-tariff rates may apply.",
                    originCountry, destinationCountry, String.join("; ", agreementNames)
            ));
        } else {
            sb.append(String.format(" There are no active trade agreements between %s and %s.", originCountry, destinationCountry));
        }

        return Map.of("answer", sb.toString());
    }

    // -----------------------------------------------
    // HELPER METHODS
    // -----------------------------------------------
    private String extractProductFromPrompt(OpenAiService service, String prompt) throws Exception {
        String extractPrompt = "Extract only the main product keyword. Return JSON: {\"product\": \"<word>\"}.";
        String json = ask(service, extractPrompt, prompt);
        return mapper.readTree(json).path("product").asText("").trim();
    }

    // 0.0: deterministic (for extraction, classification)
    private String ask(OpenAiService service, String systemPrompt, String userPrompt) {
        return ask(service, systemPrompt, userPrompt, 0.0);
    }

    // 0.2: more flexible (ex. matcha powder --> green tea --> tea)
    private String ask(OpenAiService service, String systemPrompt, String userPrompt, double temperature) {
        ChatCompletionRequest req = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(
                        new ChatMessage("system", systemPrompt),
                        new ChatMessage("user", userPrompt)
                ))
                .temperature(temperature)
                .build();
        return service.createChatCompletion(req).getChoices().get(0).getMessage().getContent();
    }

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


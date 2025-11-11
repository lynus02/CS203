package com.lynus.cs203.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.entities.TradeAgreement;
import com.lynus.cs203.repositories.TariffRepository;
import com.lynus.cs203.repositories.AgreementCountryRepository;
import com.lynus.cs203.repositories.TradeAgreementRepository;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.lynus.cs203.services.TariffCalculationService;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OpenAIService {

    private final TariffRepository tariffRepo;
    private final TradeAgreementRepository tradeRepo;
    private final AgreementCountryRepository agreementCountryRepo;
    private final TariffCalculationService calculationService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${openai.api.key:${OPENAI_API_KEY:}}")
    private String apiKey;

    public OpenAIService(TariffRepository tariffRepo, TradeAgreementRepository tradeRepo, AgreementCountryRepository agreementCountryRepo, TariffCalculationService calculationService) {
        this.tariffRepo = tariffRepo;
        this.tradeRepo = tradeRepo;
        this.agreementCountryRepo = agreementCountryRepo;
        this.calculationService = calculationService;
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
                .getContent(); // executes sql query from tariffrepo, limit to 10 results

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
        Return JSON: {"country1": "<name>", "country2": "<name>", "date": "<time>"}.
        If the date is not specified, leave it blank.
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

        String dateText = extraction.path("date").asText("").trim();
        LocalDate date = parseDateText(dateText);
        if (date == null) {
            date = LocalDate.now(); // default to today
        }

        // find all list of matching trade agreements from db
        List<Long> agreementIds = agreementCountryRepo.findAgreementsBetweenCountriesOnDate(country1, country2, date);

        if (agreementIds.isEmpty()) {
            return Map.of("answer", String.format("No trade agreement found between %s and %s.", country1, country2));
        }

        // fetch agreement details (Name + Type)
        List<TradeAgreement> agreementsList = tradeRepo.findAllByIds(agreementIds);

        List<String> agreements = agreementsList.stream()
                .map(a -> String.format("%s (%s)", a.getAgreementName(), a.getAgreementType()))
                .toList();

        List<String> effectiveDates = agreementsList.stream()
                .map(a -> a.getEffectiveDate() != null ? a.getEffectiveDate().toString() : "N/A")
                .toList();

        List<String> expirationDates = agreementsList.stream()
                .map(a -> a.getExpirationDate() != null ? a.getExpirationDate().toString() : "N/A")
                .toList();

        // summarize results
        String joinedAgreements = String.join("; ", agreements);
        String dateOfEntry = String.join("; ", effectiveDates);

        String endOfImplementation = String.join("; ", expirationDates);
        String answer = String.format(
                "The trade agreement between %s and %s is %s. The date of entry into force is %s and the end of implementation period is %s.",
                country1, country2, joinedAgreements, dateOfEntry, endOfImplementation
        );

        return Map.of("answer", answer, "agreements", joinedAgreements);
    }

    // -----------------------------------------------
    // TARIFF RATE QUERIES
    // -----------------------------------------------
    private Map<String, String> handleTariffRateQuery(OpenAiService service, String userPrompt) throws Exception {
        // extract relevant product and destination country
        String extractPrompt = """
        Extract the product, origin and destination country mentioned in the question.
        Return JSON: {"product": "<word>", "origin": "<country>", "destination": "<country>"}.
        If the country is not specified, leave it blank.
        """;

        String extractJson = ask(service, extractPrompt, userPrompt);
        JsonNode extraction = mapper.readTree(extractJson);
        String product = extraction.path("product").asText("").trim();
        String origin = normalizeCountryName(extraction.path("origin").asText("").trim());
        String destination = normalizeCountryName(extraction.path("destination").asText("").trim());

        if (product.isBlank()) {
            return Map.of("answer", "Could you rephrase that? I couldn’t detect a product name.");
        }
        if (destination.isBlank()) {
            destination = "singapore"; // default fallback
        }

        // get all possible tariffs for the destination + product
        var limit = PageRequest.of(0, 10);
        List<Tariff> tariffs = tariffRepo
                .findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(destination, product, limit)
                .getContent();

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

        // use highest tariff as the base (EIA)
        Tariff representativeTariff = tariffs.stream()
                .max(Comparator.comparingDouble(Tariff::getTariffRate))
                .orElse(tariffs.get(0));

        double baseRate = representativeTariff.getTariffRate();
        String hsCode = representativeTariff.getProduct().getProductCode();
        String sensitivityTier = calculationService.calculateSensitivityTier(hsCode);

        // find trade agreements between the two countries
        List<Long> agreementIds = agreementCountryRepo.findAgreementsBetweenCountries(origin, destination);

        double finalRate = baseRate;
        String appliedAgreement = "MFN"; // default

        if (!agreementIds.isEmpty()) {
            for (Long id : agreementIds) {
                TradeAgreement agreement = tradeRepo.findByAgreementId(id).orElse(null);
                if (agreement == null) continue;

                String[] types = agreement.getAgreementType().split("&");
                for (String type : types) {
                    double multiplier = calculationService.getDiscountMultiplier(sensitivityTier, type.trim());
                    double discountedRate = baseRate * multiplier;

                    if (discountedRate < finalRate) {
                        finalRate = discountedRate;
                        appliedAgreement = type.trim();
                    }
                }
            }
        }

        double reductionValue = baseRate - finalRate;

        String answer = String.format(
                "The tariff rate for %s imported from %s to %s is %.2f%% (base %.2f%%, Trade Agreement Reduction %.2f%%, applied: %s).",
                product, origin, destination, finalRate, baseRate, reductionValue, appliedAgreement
        );

        return Map.of("answer", answer);
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

    private LocalDate parseDateText(String dateText) {
        if (dateText == null || !dateText.isEmpty()) return null;
        try {
            return LocalDate.parse(dateText);
        } catch (java.time.format.DateTimeParseException e) {
            try {
                return LocalDate.parse(dateText, java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"));
            } catch (java.time.format.DateTimeParseException ex) {
                return null;
            }
        }
    }
}


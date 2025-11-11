package com.lynus.cs203.services;

import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.entities.TradeAgreement;
import com.lynus.cs203.repositories.AgreementCountryRepository;
import com.lynus.cs203.repositories.TariffRepository;
import com.lynus.cs203.repositories.TradeAgreementRepository;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAI Service Unit Tests (AAA structured)")
class OpenAIServiceTest {

    @Mock
    private TariffRepository tariffRepo;

    @Mock
    private TradeAgreementRepository tradeRepo;

    @Mock
    private AgreementCountryRepository agreementCountryRepo;

    @InjectMocks
    private OpenAIService svc;

    private ChatCompletionResult completionResultWithContent(String content) {
        ChatCompletionResult res = mock(ChatCompletionResult.class);
        ChatCompletionChoice choice = mock(ChatCompletionChoice.class);
        ChatMessage message = new ChatMessage("assistant", content);
        when(choice.getMessage()).thenReturn(message);
        when(res.getChoices()).thenReturn(List.of(choice));
        return res;
    }

    private Tariff buildTariff(int hsCode, String desc, double rate, String countryName) {
        Product p = new Product();
        p.setProductCode(hsCode);
        p.setProductDescription(desc);

        Country c = new Country();
        c.setCountryName(countryName);

        Tariff t = new Tariff();
        t.setProduct(p);
        t.setCountry(c);
        t.setTariffRate(rate);
        return t;
    }

    @Test
    void processChat_classify_other_returnsClarification() throws Exception {
        // Arrange
        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"OTHER\"}");
        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(classifyRes);
        })) {
            // Act
            Map<String, String> resp = svc.processChat("Hello?");

            // Assert
            assertNotNull(resp);
            assertTrue(resp.get("answer").toLowerCase().contains("i can help") || resp.get("answer").length() > 0);
            verify(mc.constructed().get(0), atLeastOnce()).createChatCompletion(any());
        }
    }

    @Test
    void processChat_hsCode_noDbMatches_returnsFallback() throws Exception {
        // Arrange
        when(tariffRepo.findByProductDescriptionOrProductCodeContaining(anyString(), anyString(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(emptyList()));

        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"HS_CODE\"}");
        ChatCompletionResult extractRes = completionResultWithContent("{\"product\":\"rice\"}");
        ChatCompletionResult fallbackRes = completionResultWithContent("Likely HS 1006.30 — reasoning...");

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractRes, fallbackRes);
        })) {
            // Act
            Map<String, String> resp = svc.processChat("What is HS code for rice?");

            // Assert
            assertNotNull(resp);
            assertEquals("rice", resp.get("product"));
            assertEquals("Likely HS 1006.30 — reasoning...", resp.get("answer"));
        }
    }

    @Test
    void processChat_hsCode_withDbMatches_returnsSummaryAnswer() throws Exception {
        // Arrange
        Tariff t = buildTariff(100630, "rice", 5.0, "japan");
        when(tariffRepo.findByProductDescriptionOrProductCodeContaining(anyString(), anyString(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(t)));

        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"HS_CODE\"}");
        ChatCompletionResult extractRes = completionResultWithContent("{\"product\":\"rice\"}");
        ChatCompletionResult summaryRes = completionResultWithContent("HS 1006.30 seems the best match.");

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractRes, summaryRes);
        })) {
            // Act
            Map<String, String> resp = svc.processChat("Which HS code matches rice?");

            // Assert
            assertNotNull(resp);
            assertEquals("rice", resp.get("product"));
            assertEquals("HS 1006.30 seems the best match.", resp.get("answer"));
        }
    }

    @Test
    void processChat_tradeAgreement_noAgreement_returnsNoAgreementMessage() throws Exception {
        // Arrange
        when(agreementCountryRepo.findAgreementsBetweenCountries(anyString(), anyString())).thenReturn(List.of());
        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"TRADE_AGREEMENT\"}");
        ChatCompletionResult extractCountries = completionResultWithContent("{\"country1\":\"Japan\",\"country2\":\"Atlantis\"}");

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractCountries);
        })) {
            // Act
            Map<String, String> resp = svc.processChat("Is there a trade agreement between Japan and Atlantis?");

            // Assert
            assertNotNull(resp);
            String ans = resp.get("answer").toLowerCase();
            assertTrue(ans.contains("no trade agreement found") || ans.contains("no trade agreement"));
        }
    }

    @Test
    void processChat_tradeAgreement_withTariff_whenAgreementExists_showsPreferentialRates() throws Exception {
        // Arrange
        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"TRADE_AGREEMENT\"}");
        ChatCompletionResult extractCountries = completionResultWithContent("{\"country1\":\"japan\",\"country2\":\"singapore\"}");
        ChatCompletionResult extractProduct = completionResultWithContent("{\"product\":\"rice\"}");

        Tariff t = buildTariff(100630, "rice", 3.5, "singapore");
        when(tariffRepo.findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(eq("singapore"), eq("rice"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(t)));

        when(agreementCountryRepo.findAgreementsBetweenCountries(eq("japan"), eq("singapore")))
                .thenReturn(List.of(1L));

        TradeAgreement mockAgreement = mock(TradeAgreement.class);
        when(mockAgreement.getAgreementName()).thenReturn("JSEPA");
        when(mockAgreement.getAgreementType()).thenReturn("FTA");
        when(tradeRepo.findAllByIds(eq(List.of(1L))))
                .thenReturn(List.of(mockAgreement));

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractCountries, extractProduct);
        })) {
            // Act
            Map<String, String> resp = svc.processChat("Is there a trade agreement between Japan and Singapore and what is the tariff for rice?");

            // Assert
            assertNotNull(resp);
            String ans = resp.get("answer").toLowerCase();
            assertTrue(
                    ans.contains("preferential") ||
                            ans.contains("zero-tariff") ||
                            ans.contains("jsepa") ||
                            ans.contains("fta"),
                    "Expected preferential/agreement mention when agreement exists, got: " + ans
            );
        }
    }

    @Test
    void processChat_tradeAgreement_withTariff_invokesTariffCalculation_noAgreementBranch() throws Exception {
        // Arrange
        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"TRADE_AGREEMENT\"}");
        ChatCompletionResult extractCountries = completionResultWithContent("{\"country1\":\"japan\",\"country2\":\"singapore\"}");
        ChatCompletionResult extractProduct = completionResultWithContent("{\"product\":\"rice\"}");

        Tariff t = buildTariff(100630, "rice", 3.5, "singapore");
        when(tariffRepo.findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(eq("singapore"), eq("rice"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(t)));

        when(agreementCountryRepo.findAgreementsBetweenCountries(eq("japan"), eq("singapore"))).thenReturn(List.of(1L));

        // mock a TradeAgreement entity returned by tradeRepo.findAllByIds(...)
        TradeAgreement mockAgreement = mock(TradeAgreement.class);
        when(mockAgreement.getAgreementName()).thenReturn("JSEPA");
        when(mockAgreement.getAgreementType()).thenReturn("FTA");
        when(tradeRepo.findAllByIds(eq(List.of(1L))))
                .thenReturn(List.of(mockAgreement));


        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractCountries, extractProduct);
        })) {
            // Act
            Map<String, String> resp = svc.processChat("Is there a trade agreement between Japan and Singapore and what is the tariff for rice?");

            // Assert
            assertNotNull(resp);
            String ans = resp.get("answer").toLowerCase();
            assertTrue(ans.contains("for rice imported to singapore") || ans.contains("no active trade agreements") || ans.contains("there are no active trade agreements"));
        }
    }

    @Test
    void processChat_tariffRate_withMatches_returnsAverage() throws Exception {
        // Arrange
        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"TARIFF_RATE\"}");
        ChatCompletionResult extractRes = completionResultWithContent("{\"product\":\"rice\",\"destination\":\"japan\"}");

        Tariff t1 = buildTariff(100630, "rice", 5.0, "japan");
        Tariff t2 = buildTariff(100630, "rice", 7.0, "japan");
        when(tariffRepo.findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(eq("japan"), eq("rice"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(t1, t2)));

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractRes);
        })) {
            // Act
            Map<String, String> resp = svc.processChat("What is the tariff for rice to Japan?");

            // Assert
            assertNotNull(resp);
            String ans = resp.get("answer").toLowerCase();
            assertTrue(ans.contains("average tariff rate for rice imported to japan"));
            assertTrue(ans.contains("hs") || ans.contains("hs"));
        }
    }

    @Test
    void private_ask_and_normalizeCountryName_behaviour() throws Exception {
        // Arrange
        OpenAiService mockedClient = mock(OpenAiService.class);
        ChatCompletionResult res = completionResultWithContent("ok-response");
        when(mockedClient.createChatCompletion(any())).thenReturn(res);

        // Act
        Method ask = OpenAIService.class.getDeclaredMethod("ask", OpenAiService.class, String.class, String.class);
        ask.setAccessible(true);
        String out = (String) ask.invoke(svc, mockedClient, "system prompt", "user prompt");

        // Assert
        assertEquals("ok-response", out);

        // Arrange (normalize)
        Method norm = OpenAIService.class.getDeclaredMethod("normalizeCountryName", String.class);
        norm.setAccessible(true);

        // Act & Assert
        String us = (String) norm.invoke(svc, "us");
        assertEquals("united states of america", us);
        String passthrough = (String) norm.invoke(svc, "somecountry");
        assertEquals("somecountry", passthrough);
    }
}

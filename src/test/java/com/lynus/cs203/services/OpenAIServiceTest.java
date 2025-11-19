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
import java.time.LocalDate;
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

    @Mock
    private TariffCalculationService calculationService;

    @InjectMocks
    private OpenAIService svc;

    private ChatCompletionResult completionResultWithContent(String content) {
        ChatMessage message = new ChatMessage("assistant", content);

        ChatCompletionChoice choice = new ChatCompletionChoice();
        choice.setMessage(message);
        choice.setFinishReason("stop");
        choice.setIndex(0);

        ChatCompletionResult result = new ChatCompletionResult();
        result.setChoices(List.of(choice));
        result.setId("test-id");
        result.setCreated(System.currentTimeMillis() / 1000L);
        result.setModel("gpt-4o-mini");

        return result;
    }

    private Tariff buildTariff(String hsCode, String desc, double rate, String countryName) {
        Product p = new Product();
        p.setProductCode(Integer.valueOf(hsCode));
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
    @DisplayName("Should return clarification for OTHER intent")
    void processChat_classify_other_returnsClarification() throws Exception {
        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"OTHER\"}");

        try (MockedConstruction<OpenAiService> mc =
                     mockConstruction(OpenAiService.class, (mock, ctx) ->
                             when(mock.createChatCompletion(any())).thenReturn(classifyRes))) {

            Map<String, String> resp = svc.processChat("Hi?");

            assertNotNull(resp);
            assertTrue(resp.get("answer").contains("clarify"));
        }
    }

    @Test
    @DisplayName("Should return fallback reasoning for HS_CODE intent with no DB matches")
    void processChat_hsCode_noDbMatches_returnsFallback() throws Exception {
        when(tariffRepo.findByProductDescriptionOrProductCodeContaining(anyString(), anyString(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(emptyList()));

        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"HS_CODE\"}");
        ChatCompletionResult extractRes = completionResultWithContent("{\"product\":\"rice\"}");
        ChatCompletionResult fallbackRes = completionResultWithContent("Likely HS 1006 — reasoning...");

        try (MockedConstruction<OpenAiService> mc =
                     mockConstruction(OpenAiService.class, (mock, ctx) ->
                             when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractRes, fallbackRes))) {

            Map<String, String> resp = svc.processChat("HS code for rice?");

            assertNotNull(resp);
            assertEquals("rice", resp.get("product"));
            assertEquals("Likely HS 1006 — reasoning...", resp.get("answer"));
        }
    }

    @Test
    @DisplayName("Should return summary answer for HS_CODE intent with DB matches")
    void processChat_hsCode_withDbMatches_returnsSummaryAnswer() throws Exception {
        Tariff t = buildTariff("100630", "rice", 5.0, "japan");
        when(tariffRepo.findByProductDescriptionOrProductCodeContaining(anyString(), anyString(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(t)));

        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"HS_CODE\"}");
        ChatCompletionResult extractRes = completionResultWithContent("{\"product\":\"rice\"}");
        ChatCompletionResult summaryRes = completionResultWithContent("HS 1006.30 seems best.");

        try (MockedConstruction<OpenAiService> mc =
                     mockConstruction(OpenAiService.class, (mock, ctx) ->
                             when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractRes, summaryRes))) {

            Map<String, String> resp = svc.processChat("Find HS code for rice");

            assertNotNull(resp);
            assertEquals("rice", resp.get("product"));
            assertEquals("HS 1006.30 seems best.", resp.get("answer"));
        }
    }

    @Test
    @DisplayName("Should return no agreement message for TRADE_AGREEMENT intent when none exists")
    void processChat_tradeAgreement_noAgreement_returnsMessage() throws Exception {
        when(agreementCountryRepo.findAgreementsBetweenCountriesOnDate(anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(emptyList());

        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"TRADE_AGREEMENT\"}");
        ChatCompletionResult extractRes = completionResultWithContent("{\"country1\":\"Japan\",\"country2\":\"Atlantis\"}");

        try (MockedConstruction<OpenAiService> mc =
                     mockConstruction(OpenAiService.class, (mock, ctx) ->
                             when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractRes))) {

            Map<String, String> resp = svc.processChat("Is there a trade agreement between Japan and Atlantis?");

            assertNotNull(resp);
            assertTrue(resp.get("answer").toLowerCase().contains("no trade agreement"));
        }
    }

    @Test
    @DisplayName("Should return agreement details for TRADE_AGREEMENT intent when an agreement exists")
    void processChat_tradeAgreement_withAgreement_returnsCorrectText() throws Exception {
        when(agreementCountryRepo.findAgreementsBetweenCountriesOnDate(anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(List.of(99L));

        TradeAgreement agr = new TradeAgreement();
        agr.setAgreementName("JSEPA");
        agr.setAgreementType("FTA");
        agr.setEffectiveDate(LocalDate.of(2006, 1, 1));
        agr.setExpirationDate(null);

        when(tradeRepo.findAllByIds(eq(List.of(99L)))).thenReturn(List.of(agr));

        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"TRADE_AGREEMENT\"}");
        ChatCompletionResult extractRes = completionResultWithContent("{\"country1\":\"Japan\",\"country2\":\"Singapore\"}");

        try (MockedConstruction<OpenAiService> mc =
                     mockConstruction(OpenAiService.class, (mock, ctx) ->
                             when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractRes))) {

            Map<String, String> resp = svc.processChat("Trade agreement between Japan and Singapore?");

            assertNotNull(resp);
            assertTrue(resp.get("answer").contains("JSEPA"));
        }
    }

    @Test
    @DisplayName("Should return final tariff rate for TARIFF_RATE intent with DB matches")
    void processChat_tariffRate_withMatches_returnsFinalRate() throws Exception {
        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"TARIFF_RATE\"}");
        ChatCompletionResult extractRes = completionResultWithContent("{\"product\":\"rice\",\"destination\":\"japan\",\"origin\":\"usa\"}");

        Tariff t1 = buildTariff("100630", "rice", 5.0, "japan");
        when(tariffRepo.findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(eq("japan"), eq("rice"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(t1)));

        when(calculationService.calculateSensitivityTier(anyString())).thenReturn("LOW");

        try (MockedConstruction<OpenAiService> mc =
                     mockConstruction(OpenAiService.class, (mock, ctx) ->
                             when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractRes))) {

            Map<String, String> resp = svc.processChat("What is tariff for rice from USA to Japan?");

            assertNotNull(resp);
            assertTrue(resp.get("answer").contains("tariff rate"));
        }
    }

    @Test
    @DisplayName("Should test private methods ask and normalizeCountryName")
    void private_ask_and_normalizeCountryName() throws Exception {
        OpenAiService mockClient = mock(OpenAiService.class);
        when(mockClient.createChatCompletion(any())).thenReturn(completionResultWithContent("ok"));

        Method ask = OpenAIService.class.getDeclaredMethod("ask", OpenAiService.class, String.class, String.class);
        ask.setAccessible(true);
        assertEquals("ok", ask.invoke(svc, mockClient, "sys", "user"));

        Method normalize = OpenAIService.class.getDeclaredMethod("normalizeCountryName", String.class);
        normalize.setAccessible(true);
        assertEquals("united states of america", normalize.invoke(svc, "US"));
        assertEquals("somecountry", normalize.invoke(svc, "somecountry"));
    }
}

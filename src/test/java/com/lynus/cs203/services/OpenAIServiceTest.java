// java
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

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAI Sevice Unit Tests")
class OpenAIServiceTest {

    @Mock
    private TariffRepository tariffRepo;

    @Mock
    private TradeAgreementRepository tradeRepo;

    @Mock
    private AgreementCountryRepository agreementCountryRepo;

    @InjectMocks
    private OpenAIService svc;

    // helper: build a ChatCompletionResult whose first choice's message content is the provided string
    private ChatCompletionResult completionResultWithContent(String content) {
        ChatCompletionResult res = mock(ChatCompletionResult.class);
        ChatCompletionChoice choice = mock(ChatCompletionChoice.class);
        ChatMessage message = new ChatMessage("assistant", content);
        when(choice.getMessage()).thenReturn(message);
        when(res.getChoices()).thenReturn(List.of(choice));
        return res;
    }

    private Tariff buildTariff(int hsCodeAsInt, String desc, double rate, String countryName) {
        Product p = new Product();
        p.setProductCode(hsCodeAsInt); // Integer expected by production code
        p.setProductDescription(desc);

        Country c = new Country();
        c.setCountryName(countryName);

        Tariff t = new Tariff();
        t.setProduct(p);
        t.setCountry(c);
        t.setTariffRate(rate);
        return t;
    }


    // helper: create a mocked Tariff with provided fields
    private Tariff mockTariff(int hsCodeAsInt, String desc, double rate, String countryName) {
        Tariff t = mock(Tariff.class);
        Product p = mock(Product.class);
        Country c = mock(Country.class);

        when(p.getProductCode()).thenReturn(hsCodeAsInt); // return Integer
        when(p.getProductDescription()).thenReturn(desc);
        when(c.getCountryName()).thenReturn(countryName);

        when(t.getProduct()).thenReturn(p);
        when(t.getTariffRate()).thenReturn(rate);
        when(t.getCountry()).thenReturn(c);
        return t;
    }

    @Test
    void processChat_whenIntentOther_returnsClarificationPrompt() throws Exception {
        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"OTHER\"}");

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, context) -> {
            when(mock.createChatCompletion(any())).thenReturn(classifyRes);
        })) {
            var resp = svc.processChat("Tell me a joke");

            assertNotNull(resp);
            assertTrue(resp.get("answer").toLowerCase().contains("hs codes") || resp.get("answer").length() > 0);

            OpenAiService created = mc.constructed().get(0);
            verify(created, times(1)).createChatCompletion(any());
        }
    }

    @Test
    void processChat_hsCode_noDbMatches_returnsFallbackAnswerAndProduct() throws Exception {
        when(tariffRepo.findByProductDescriptionOrProductCodeContaining(anyString(), anyString(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(emptyList()));

        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"HS_CODE\"}");
        ChatCompletionResult extractRes = completionResultWithContent("{\"product\":\"rice\"}");
        ChatCompletionResult fallbackRes = completionResultWithContent("Likely HS 1006.30 — reasoning...");

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractRes, fallbackRes);
        })) {
            Map<String, String> resp = svc.processChat("What is the HS code for rice?");

            assertNotNull(resp);
            assertEquals("rice", resp.get("product"));
            assertEquals("Likely HS 1006.30 — reasoning...", resp.get("answer"));

            OpenAiService created = mc.constructed().get(0);
            verify(created, times(3)).createChatCompletion(any());
            verify(tariffRepo, times(1)).findByProductDescriptionOrProductCodeContaining(eq("rice"), eq("rice"), any(PageRequest.class));
        }
    }

    @Test
    void processChat_hsCode_withDbMatches_returnsSummaryAndProduct() throws Exception {
        Tariff t = buildTariff(100630, "rice", 5.0, "japan");
        when(tariffRepo.findByProductDescriptionOrProductCodeContaining(anyString(), anyString(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(t)));

        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"HS_CODE\"}");
        ChatCompletionResult extractRes = completionResultWithContent("{\"product\":\"rice\"}");
        ChatCompletionResult summaryRes = completionResultWithContent("HS 1006.30 seems the best match.");

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractRes, summaryRes);
        })) {
            Map<String, String> resp = svc.processChat("Which HS code matches rice?");

            assertNotNull(resp);
            assertEquals("rice", resp.get("product"));
            assertEquals("HS 1006.30 seems the best match.", resp.get("answer"));

            OpenAiService created = mc.constructed().get(0);
            verify(tariffRepo, times(1)).findByProductDescriptionOrProductCodeContaining(eq("rice"), eq("rice"), any(PageRequest.class));
            verify(created, times(3)).createChatCompletion(any());
        }
    }

    @Test
    void processChat_tariffRate_noMatches_returnsGptFallback() throws Exception {
        when(tariffRepo.findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(anyString(), anyString(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(emptyList()));

        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"TARIFF_RATE\"}");
        ChatCompletionResult extractRes = completionResultWithContent("{\"product\":\"rice\",\"destination\":\"japan\"}");
        ChatCompletionResult fallbackRes = completionResultWithContent("Estimated tariff range: 5-10% based on category.");

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractRes, fallbackRes);
        })) {
            Map<String, String> resp = svc.processChat("What is the tariff rate for rice imported to Japan?");

            assertNotNull(resp);
            assertEquals("Estimated tariff range: 5-10% based on category.", resp.get("answer"));

            OpenAiService created = mc.constructed().get(0);
            verify(created, times(3)).createChatCompletion(any());
            verify(tariffRepo, times(1)).findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(eq("japan"), eq("rice"), any(PageRequest.class));
        }
    }

    @Test
    void processChat_tariffRate_withMatches_returnsAvgTariff() throws Exception {
        Tariff t1 = buildTariff(100630, "rice", 3.0, "japan");
        Tariff t2 = buildTariff(100630, "rice", 5.0, "japan");
        when(tariffRepo.findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(eq("japan"), eq("rice"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(t1, t2)));

        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"TARIFF_RATE\"}");
        ChatCompletionResult extractRes = completionResultWithContent("{\"product\":\"rice\",\"destination\":\"japan\"}");

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractRes);
        })) {
            Map<String, String> resp = svc.processChat("What is the tariff for rice imported to Japan?");

            assertNotNull(resp);
            String answer = resp.get("answer");
            assertTrue(answer.toLowerCase().contains("average tariff rate"));
            assertTrue(answer.contains("4.00") || answer.contains("4.0"));

            OpenAiService created = mc.constructed().get(0);
            verify(tariffRepo, times(1)).findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(eq("japan"), eq("rice"), any(PageRequest.class));
            verify(created, times(2)).createChatCompletion(any());
        }
    }

    @Test
    void processChat_tradeAgreement_noAgreements_returnsNoAgreementMessage() throws Exception {
        when(agreementCountryRepo.findAgreementsBetweenCountries("japan", "singapore"))
                .thenReturn(List.of());

        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"TRADE_AGREEMENT\"}");
        ChatCompletionResult extractRes = completionResultWithContent("{\"country1\":\"Japan\",\"country2\":\"Singapore\"}");

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractRes);
        })) {
            Map<String, String> resp = svc.processChat("Is there a free trade agreement between Japan and Singapore?");

            assertNotNull(resp);
            assertEquals("No trade agreement found between japan and singapore.", resp.get("answer"));

            OpenAiService created = mc.constructed().get(0);
            verify(agreementCountryRepo, times(1)).findAgreementsBetweenCountries("japan", "singapore");
            verify(created, times(2)).createChatCompletion(any());
        }
    }

    @Test
    void processChat_tradeAgreement_withAgreements_and_tariffCombines_returnsCombinedAnswerOrTariff() throws Exception {
        when(agreementCountryRepo.findAgreementsBetweenCountries("japan", "singapore"))
                .thenReturn(List.of(1L));

        TradeAgreement ta = mock(TradeAgreement.class);
        when(ta.getAgreementName()).thenReturn("JP-SG FTA");
        when(ta.getAgreementType()).thenReturn("FTA");
        when(tradeRepo.findAllByIds(List.of(1L))).thenReturn(List.of(ta));

        Tariff tariff = buildTariff(100630, "rice", 3.0, "singapore");
        when(tariffRepo.findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(eq("singapore"), eq("rice"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(tariff)));

        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"TRADE_AGREEMENT\"}");
        ChatCompletionResult extractCountries = completionResultWithContent("{\"country1\":\"Japan\",\"country2\":\"Singapore\"}");
        ChatCompletionResult extractProduct = completionResultWithContent("{\"product\":\"rice\"}");

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractCountries, extractProduct);
        })) {
            Map<String, String> resp = svc.processChat("Is there a trade agreement and what's the tariff for rice between Japan and Singapore?");

            assertNotNull(resp);
            String answer = resp.get("answer");
            assertNotNull(answer);
            assertTrue(answer.toLowerCase().contains("jp-sg fta") || answer.contains("JP-SG FTA") || answer.toLowerCase().contains("average tariff"));

            OpenAiService created = mc.constructed().get(0);
            verify(agreementCountryRepo, times(2)).findAgreementsBetweenCountries("japan", "singapore");
            verify(tradeRepo, times(2)).findAllByIds(List.of(1L));
            verify(tariffRepo, atLeastOnce()).findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(eq("singapore"), eq("rice"), any(PageRequest.class));
            verify(created, times(3)).createChatCompletion(any());
        }
    }

    @Test
    void processChat_tradeAgreement_withAgreements_but_noTariffData_returnsNoTariffDataMessage() throws Exception {
        when(agreementCountryRepo.findAgreementsBetweenCountries("japan", "singapore")).thenReturn(List.of(1L));
        TradeAgreement ta = mock(TradeAgreement.class);
        when(ta.getAgreementName()).thenReturn("JP-SG FTA");
        when(ta.getAgreementType()).thenReturn("FTA");
        when(tradeRepo.findAllByIds(List.of(1L))).thenReturn(List.of(ta));

        when(tariffRepo.findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(eq("singapore"), eq("rice"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(emptyList()));

        ChatCompletionResult classifyRes = completionResultWithContent("{\"intent\":\"TRADE_AGREEMENT\"}");
        ChatCompletionResult extractCountries = completionResultWithContent("{\"country1\":\"Japan\",\"country2\":\"Singapore\"}");
        ChatCompletionResult extractProduct = completionResultWithContent("{\"product\":\"rice\"}");

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(classifyRes, extractCountries, extractProduct);
        })) {
            Map<String, String> resp = svc.processChat("Is there a trade agreement and what's the tariff for rice between Japan and Singapore?");

            assertNotNull(resp);
            assertTrue(resp.get("answer").toLowerCase().contains("no tariff data found"));

            OpenAiService created = mc.constructed().get(0);
            verify(tariffRepo, times(1)).findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(eq("singapore"), eq("rice"), any(PageRequest.class));
            verify(created, times(3)).createChatCompletion(any());
        }
    }
}

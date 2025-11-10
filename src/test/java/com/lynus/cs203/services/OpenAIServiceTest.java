package com.lynus.cs203.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.TariffRepository;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAI Service Unit Tests (updated)")
class OpenAIServiceTest {

    @Mock
    private TariffRepository tariffRepo;

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
        p.setProductCode(hsCodeAsInt);
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
    void processChat_extractBlank_returnsClarification() throws Exception {
        ChatCompletionResult extractRes = completionResultWithContent("{\"product\":\"\"}");

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(extractRes);
        })) {
            Map<String, String> resp = svc.processChat("I want something");

            assertNotNull(resp);
            assertTrue(resp.get("answer").toLowerCase().contains("could you rephrase")
                    || resp.get("answer").length() > 0);

            OpenAiService created = mc.constructed().get(0);
            verify(created, times(1)).createChatCompletion(any());
        }
    }

    @Test
    void processChat_noDbMatches_returnsFallback() throws Exception {
        when(tariffRepo.findByProduct_ProductDescriptionContainingIgnoreCaseOrProduct_ProductCode(anyString(), anyInt(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(emptyList()));

        ChatCompletionResult extractRes = completionResultWithContent("{\"product\":\"rice\"}");
        ChatCompletionResult fallbackRes = completionResultWithContent("Likely HS 1006.30 — reasoning...");

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(extractRes, fallbackRes);
        })) {
            Map<String, String> resp = svc.processChat("What is the HS code for rice?");

            assertNotNull(resp);
            assertEquals("rice", resp.get("product"));
            assertEquals("Likely HS 1006.30 — reasoning...", resp.get("answer"));

            OpenAiService created = mc.constructed().get(0);
            verify(created, times(2)).createChatCompletion(any());
            verify(tariffRepo, times(1)).findByProduct_ProductDescriptionContainingIgnoreCaseOrProduct_ProductCode(eq("rice"), anyInt(), any(PageRequest.class));
        }
    }

    @Test
    void processChat_withDbMatches_returnsSummaryAnswer() throws Exception {
        Tariff t = buildTariff(100630, "rice", 5.0, "japan");
        when(tariffRepo.findByProduct_ProductDescriptionContainingIgnoreCaseOrProduct_ProductCode(anyString(), anyInt(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(t)));

        ChatCompletionResult extractRes = completionResultWithContent("{\"product\":\"rice\"}");
        ChatCompletionResult summaryRes = completionResultWithContent("HS 1006.30 seems the best match.");

        try (MockedConstruction<OpenAiService> mc = mockConstruction(OpenAiService.class, (mock, ctx) -> {
            when(mock.createChatCompletion(any())).thenReturn(extractRes, summaryRes);
        })) {
            Map<String, String> resp = svc.processChat("Which HS code matches rice?");

            assertNotNull(resp);
            assertEquals("rice", resp.get("product"));
            assertEquals("HS 1006.30 seems the best match.", resp.get("answer"));

            OpenAiService created = mc.constructed().get(0);
            verify(created, times(2)).createChatCompletion(any());
            verify(tariffRepo, times(1)).findByProduct_ProductDescriptionContainingIgnoreCaseOrProduct_ProductCode(eq("rice"), anyInt(), any(PageRequest.class));
        }
    }

    @Test
    void private_safeParseInt_and_similarityScore_behaviour() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> cls = OpenAIService.class;
        Method safeParse = cls.getDeclaredMethod("safeParseInt", String.class);
        safeParse.setAccessible(true);

        assertEquals(123, safeParse.invoke(null, "123"));
        assertEquals(456, safeParse.invoke(null, "abc456def"));
        assertEquals(0, safeParse.invoke(null, "no digits"));

        Method sim = cls.getDeclaredMethod("similarityScore", String.class, String.class);
        sim.setAccessible(true);

        double s1 = (double) sim.invoke(null, "rice grain", "rice");
        double s2 = (double) sim.invoke(null, "completely different", "nothing shared");
        assertTrue(s1 > 0);
        assertEquals(0.0, s2, 1e-9);
    }

    @Test
    void private_getSynonym_usesOpenAiResponse() throws Exception {
        // prepare a mocked OpenAiService that returns a single-word synonym
        OpenAiService mockedClient = mock(OpenAiService.class);
        ChatCompletionResult synonymRes = completionResultWithContent("grain");
        when(mockedClient.createChatCompletion(any())).thenReturn(synonymRes);

        // invoke private method getSynonym via reflection
        Method getSyn = OpenAIService.class.getDeclaredMethod("getSynonym", OpenAiService.class, String.class);
        getSyn.setAccessible(true);

        String result = (String) getSyn.invoke(svc, mockedClient, "rice");
        assertEquals("grain", result);
        verify(mockedClient, times(1)).createChatCompletion(any());
    }
}

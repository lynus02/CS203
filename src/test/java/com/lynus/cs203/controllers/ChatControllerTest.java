package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.ChatRequest;
import com.lynus.cs203.services.OpenAIService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ChatControllerTest {

    @Test
    void returnsBadRequestWhenPromptIsNull() {
        OpenAIService mockService = mock(OpenAIService.class);
        ChatController controller = new ChatController(mockService);

        ChatRequest req = mock(ChatRequest.class);
        when(req.getPrompt()).thenReturn(null);

        ResponseEntity<Map<String, String>> resp = controller.chat(req);

        assertEquals(400, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertEquals("Prompt cannot be null or empty", resp.getBody().get("error"));
    }

    @Test
    void returnsBadRequestWhenPromptIsBlank() {
        OpenAIService mockService = mock(OpenAIService.class);
        ChatController controller = new ChatController(mockService);

        ChatRequest req = mock(ChatRequest.class);
        when(req.getPrompt()).thenReturn("   ");

        ResponseEntity<Map<String, String>> resp = controller.chat(req);

        assertEquals(400, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertEquals("Prompt cannot be null or empty", resp.getBody().get("error"));
    }

    @Test
    void returnsOkWhenServiceReturnsAnswer() throws Exception {
        OpenAIService mockService = mock(OpenAIService.class);
        ChatController controller = new ChatController(mockService);

        ChatRequest req = mock(ChatRequest.class);
        String prompt = "What is the HS code for rice?";
        when(req.getPrompt()).thenReturn(prompt);

        Map<String, String> serviceResult = Map.of("answer", "HS 1006.30");
        when(mockService.processChat(prompt)).thenReturn(serviceResult);

        ResponseEntity<Map<String, String>> resp = controller.chat(req);

        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertEquals("HS 1006.30", resp.getBody().get("answer"));
        verify(mockService, times(1)).processChat(prompt);
    }

    @Test
    void returnsInternalServerErrorWhenServiceThrows() throws Exception {
        OpenAIService mockService = mock(OpenAIService.class);
        ChatController controller = new ChatController(mockService);

        ChatRequest req = mock(ChatRequest.class);
        String prompt = "Trigger error";
        when(req.getPrompt()).thenReturn(prompt);

        when(mockService.processChat(prompt)).thenThrow(new RuntimeException("upstream failure"));

        ResponseEntity<Map<String, String>> resp = controller.chat(req);

        assertEquals(500, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().get("error").contains("upstream failure"));
        verify(mockService, times(1)).processChat(prompt);
    }
}

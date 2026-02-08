package com.hubz.infrastructure.ollama;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OllamaAdapter.
 */
@ExtendWith(MockitoExtension.class)
class OllamaAdapterTest {

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private OllamaAdapter ollamaAdapter;

    private static final String OLLAMA_URL = "http://localhost:11434";
    private static final String MODEL = "llama3.1";

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // Setup RestTemplateBuilder mock chain
        lenient().when(restTemplateBuilder.requestFactory(any(Supplier.class))).thenReturn(restTemplateBuilder);
        lenient().when(restTemplateBuilder.build()).thenReturn(restTemplate);
    }

    @Test
    void shouldReturnNullWhenOllamaIsDisabled() {
        // Given
        ollamaAdapter = new OllamaAdapter(restTemplateBuilder, OLLAMA_URL, MODEL, false, 30000);

        // When
        String result = ollamaAdapter.generateResponse("Hello", "System prompt");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnFalseForIsAvailableWhenDisabled() {
        // Given
        ollamaAdapter = new OllamaAdapter(restTemplateBuilder, OLLAMA_URL, MODEL, false, 30000);

        // When
        boolean available = ollamaAdapter.isAvailable();

        // Then
        assertThat(available).isFalse();
    }

    @Test
    void shouldReturnModelName() {
        // Given
        ollamaAdapter = new OllamaAdapter(restTemplateBuilder, OLLAMA_URL, MODEL, true, 30000);

        // When
        String modelName = ollamaAdapter.getModelName();

        // Then
        assertThat(modelName).isEqualTo(MODEL);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnTrueWhenOllamaIsAvailable() {
        // Given
        ollamaAdapter = new OllamaAdapter(restTemplateBuilder, OLLAMA_URL, MODEL, true, 30000);

        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(Map.of("models", "[]"), HttpStatus.OK);
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), any(Class.class))).thenReturn(response);

        // When
        boolean available = ollamaAdapter.isAvailable();

        // Then
        assertThat(available).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnFalseWhenOllamaConnectionFails() {
        // Given
        ollamaAdapter = new OllamaAdapter(restTemplateBuilder, OLLAMA_URL, MODEL, true, 30000);

        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), any(Class.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // When
        boolean available = ollamaAdapter.isAvailable();

        // Then
        assertThat(available).isFalse();
    }

    @Test
    void shouldGenerateResponseSuccessfully() {
        // Given
        ollamaAdapter = new OllamaAdapter(restTemplateBuilder, OLLAMA_URL, MODEL, true, 30000);

        OllamaAdapter.OllamaResponse ollamaResponse = new OllamaAdapter.OllamaResponse();
        ollamaResponse.setResponse("{\"intent\":\"CREATE_TASK\",\"title\":\"Test\"}");
        ollamaResponse.setDone(true);

        ResponseEntity<OllamaAdapter.OllamaResponse> response = new ResponseEntity<>(ollamaResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                eq(OLLAMA_URL + "/api/generate"),
                any(HttpEntity.class),
                eq(OllamaAdapter.OllamaResponse.class)
        )).thenReturn(response);

        // When
        String result = ollamaAdapter.generateResponse("Create a task", "System prompt");

        // Then
        assertThat(result).isEqualTo("{\"intent\":\"CREATE_TASK\",\"title\":\"Test\"}");
    }

    @Test
    void shouldReturnNullWhenOllamaReturnsEmptyResponse() {
        // Given
        ollamaAdapter = new OllamaAdapter(restTemplateBuilder, OLLAMA_URL, MODEL, true, 30000);

        OllamaAdapter.OllamaResponse ollamaResponse = new OllamaAdapter.OllamaResponse();
        ollamaResponse.setResponse(null);

        ResponseEntity<OllamaAdapter.OllamaResponse> response = new ResponseEntity<>(ollamaResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                eq(OLLAMA_URL + "/api/generate"),
                any(HttpEntity.class),
                eq(OllamaAdapter.OllamaResponse.class)
        )).thenReturn(response);

        // When
        String result = ollamaAdapter.generateResponse("Test", "System");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenOllamaThrowsException() {
        // Given
        ollamaAdapter = new OllamaAdapter(restTemplateBuilder, OLLAMA_URL, MODEL, true, 30000);

        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(OllamaAdapter.OllamaResponse.class)
        )).thenThrow(new RestClientException("Timeout"));

        // When
        String result = ollamaAdapter.generateResponse("Test", "System");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldIncludeConversationHistoryInPrompt() {
        // Given
        ollamaAdapter = new OllamaAdapter(restTemplateBuilder, OLLAMA_URL, MODEL, true, 30000);

        OllamaAdapter.OllamaResponse ollamaResponse = new OllamaAdapter.OllamaResponse();
        ollamaResponse.setResponse("{\"intent\":\"CREATE_TASK\"}");
        ollamaResponse.setDone(true);

        ResponseEntity<OllamaAdapter.OllamaResponse> response = new ResponseEntity<>(ollamaResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                eq(OLLAMA_URL + "/api/generate"),
                any(HttpEntity.class),
                eq(OllamaAdapter.OllamaResponse.class)
        )).thenReturn(response);

        // When
        String conversationHistory = "User: Previous message\nAssistant: Previous response";
        String result = ollamaAdapter.generateResponseWithHistory("New message", "System", conversationHistory);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void shouldHandleNullResponseBody() {
        // Given
        ollamaAdapter = new OllamaAdapter(restTemplateBuilder, OLLAMA_URL, MODEL, true, 30000);

        ResponseEntity<OllamaAdapter.OllamaResponse> response = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.postForEntity(
                eq(OLLAMA_URL + "/api/generate"),
                any(HttpEntity.class),
                eq(OllamaAdapter.OllamaResponse.class)
        )).thenReturn(response);

        // When
        String result = ollamaAdapter.generateResponse("Test", "System");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldTrimResponseWhitespace() {
        // Given
        ollamaAdapter = new OllamaAdapter(restTemplateBuilder, OLLAMA_URL, MODEL, true, 30000);

        OllamaAdapter.OllamaResponse ollamaResponse = new OllamaAdapter.OllamaResponse();
        ollamaResponse.setResponse("  {\"intent\":\"CREATE_TASK\"}  \n");
        ollamaResponse.setDone(true);

        ResponseEntity<OllamaAdapter.OllamaResponse> response = new ResponseEntity<>(ollamaResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                eq(OLLAMA_URL + "/api/generate"),
                any(HttpEntity.class),
                eq(OllamaAdapter.OllamaResponse.class)
        )).thenReturn(response);

        // When
        String result = ollamaAdapter.generateResponse("Test", "System");

        // Then
        assertThat(result).isEqualTo("{\"intent\":\"CREATE_TASK\"}");
    }
}

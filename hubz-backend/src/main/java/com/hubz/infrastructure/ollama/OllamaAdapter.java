package com.hubz.infrastructure.ollama;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubz.application.port.out.OllamaPort;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Adapter implementation for Ollama LLM API communication.
 * Implements the OllamaPort interface from the application layer.
 */
@Component
@Slf4j
public class OllamaAdapter implements OllamaPort {

    private final RestTemplate restTemplate;
    private final String ollamaUrl;
    private final String model;
    private final boolean enabled;

    public OllamaAdapter(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${ollama.url:http://localhost:11434}") String ollamaUrl,
            @Value("${ollama.model:llama3.1}") String model,
            @Value("${ollama.enabled:true}") boolean enabled,
            @Value("${ollama.timeout:30000}") int timeout) {

        this.ollamaUrl = ollamaUrl;
        this.model = model;
        this.enabled = enabled;

        // Configure timeouts using SimpleClientHttpRequestFactory
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(timeout);

        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> factory)
                .build();

        log.info("OllamaAdapter initialized - URL: {}, Model: {}, Enabled: {}", ollamaUrl, model, enabled);
    }

    @Override
    public String generateResponse(String prompt, String systemPrompt) {
        return generateResponseWithHistory(prompt, systemPrompt, null);
    }

    @Override
    public String generateResponseWithHistory(String prompt, String systemPrompt, String conversationHistory) {
        if (!enabled) {
            log.debug("Ollama is disabled by configuration");
            return null;
        }

        try {
            String fullPrompt = buildFullPrompt(prompt, conversationHistory);

            OllamaRequest request = new OllamaRequest();
            request.setModel(model);
            request.setPrompt(fullPrompt);
            request.setSystem(systemPrompt);
            request.setStream(false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<OllamaRequest> entity = new HttpEntity<>(request, headers);

            String url = ollamaUrl + "/api/generate";
            log.debug("Sending request to Ollama: {}", url);

            ResponseEntity<OllamaResponse> response = restTemplate.postForEntity(
                    url,
                    entity,
                    OllamaResponse.class
            );

            if (response.getBody() != null && response.getBody().getResponse() != null) {
                String result = response.getBody().getResponse().trim();
                log.debug("Ollama response received (length: {} chars)", result.length());
                return result;
            }

            log.warn("Ollama returned empty response");
            return null;

        } catch (RestClientException e) {
            log.error("Failed to communicate with Ollama: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error calling Ollama", e);
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isAvailable() {
        if (!enabled) {
            return false;
        }

        try {
            String url = ollamaUrl + "/api/tags";
            ResponseEntity<Map<String, Object>> response = restTemplate.getForEntity(url,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);
            boolean available = response.getStatusCode().is2xxSuccessful();
            log.debug("Ollama availability check: {}", available);
            return available;
        } catch (Exception e) {
            log.debug("Ollama not available: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getModelName() {
        return model;
    }

    private String buildFullPrompt(String prompt, String conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return prompt;
        }
        return conversationHistory + "\nNouveau message utilisateur: " + prompt;
    }

    /**
     * Request body for Ollama API.
     */
    @Data
    static class OllamaRequest {
        private String model;
        private String prompt;
        private String system;
        private boolean stream;
    }

    /**
     * Response body from Ollama API.
     */
    @Data
    static class OllamaResponse {
        private String model;
        private String response;
        @JsonProperty("created_at")
        private String createdAt;
        private boolean done;
        private Long context;
        @JsonProperty("total_duration")
        private Long totalDuration;
        @JsonProperty("load_duration")
        private Long loadDuration;
        @JsonProperty("prompt_eval_count")
        private Integer promptEvalCount;
        @JsonProperty("prompt_eval_duration")
        private Long promptEvalDuration;
        @JsonProperty("eval_count")
        private Integer evalCount;
        @JsonProperty("eval_duration")
        private Long evalDuration;
    }
}

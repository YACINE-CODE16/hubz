package com.hubz.application.service.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubz.application.port.in.JobExecutor;
import com.hubz.domain.enums.JobType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Executor for WEBHOOK_CALL jobs.
 * Sends HTTP POST requests to configured webhook URLs.
 *
 * Payload format:
 * {
 *   "url": "https://example.com/webhook",
 *   "body": "{ ... }",
 *   "headers": { "X-Custom": "value" }
 * }
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookJobExecutor implements JobExecutor {

    private final ObjectMapper objectMapper;

    private static final int TIMEOUT_SECONDS = 30;
    private static final int MAX_RESPONSE_STATUS = 299;

    @Override
    public void execute(String payload) throws Exception {
        JsonNode node = objectMapper.readTree(payload);
        String url = node.get("url").asText();
        String body = node.has("body") ? node.get("body").toString() : "{}";

        log.info("Sending webhook to: {}", url);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        // Add custom headers if provided
        if (node.has("headers")) {
            JsonNode headers = node.get("headers");
            headers.fields().forEachRemaining(entry ->
                    requestBuilder.header(entry.getKey(), entry.getValue().asText())
            );
        }

        HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() > MAX_RESPONSE_STATUS) {
            throw new RuntimeException("Webhook call failed with status " + response.statusCode() + ": " + response.body());
        }

        log.info("Webhook delivered successfully to {} with status {}", url, response.statusCode());
    }

    @Override
    public JobType getJobType() {
        return JobType.WEBHOOK_CALL;
    }
}

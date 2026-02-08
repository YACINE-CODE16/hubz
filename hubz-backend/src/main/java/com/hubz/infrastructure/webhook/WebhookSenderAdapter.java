package com.hubz.infrastructure.webhook;

import com.hubz.application.port.out.WebhookSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookSenderAdapter implements WebhookSenderPort {

    private final RestTemplate webhookRestTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public int send(String url, Map<String, Object> payload, String secret) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "Hubz-Webhook/1.0");

            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Add HMAC signature if secret is provided
            if (secret != null && !secret.isBlank()) {
                String signature = computeHmacSha256(jsonPayload, secret);
                headers.set("X-Hubz-Signature", "sha256=" + signature);
            }

            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<String> response = webhookRestTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            return response.getStatusCode().value();
        } catch (Exception e) {
            log.error("Failed to send webhook to {}: {}", url, e.getMessage());
            throw new RuntimeException("Webhook delivery failed: " + e.getMessage(), e);
        }
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC signature", e);
        }
    }
}

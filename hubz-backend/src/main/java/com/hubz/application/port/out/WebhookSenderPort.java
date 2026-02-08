package com.hubz.application.port.out;

import java.util.Map;

/**
 * Port for sending HTTP webhook requests.
 * Infrastructure layer provides the actual HTTP client implementation.
 */
public interface WebhookSenderPort {

    /**
     * Sends a webhook payload to the specified URL.
     *
     * @param url     the webhook URL to send to
     * @param payload the JSON payload as a Map
     * @param secret  optional HMAC secret for signing the payload (can be null)
     * @return the HTTP status code returned by the target
     */
    int send(String url, Map<String, Object> payload, String secret);
}

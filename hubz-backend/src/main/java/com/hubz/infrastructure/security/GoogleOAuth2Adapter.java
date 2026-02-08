package com.hubz.infrastructure.security;

import com.hubz.application.port.out.GoogleOAuth2Port;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Infrastructure adapter that communicates with Google OAuth2 API.
 * Implements the GoogleOAuth2Port defined in the application layer.
 */
@Component
public class GoogleOAuth2Adapter implements GoogleOAuth2Port {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final RestTemplate restTemplate;

    public GoogleOAuth2Adapter(
            @Value("${app.oauth2.google.client-id}") String clientId,
            @Value("${app.oauth2.google.client-secret}") String clientSecret,
            @Value("${app.oauth2.google.redirect-uri}") String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String buildAuthorizationUrl() {
        return GOOGLE_AUTH_URL
                + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode("openid email profile", StandardCharsets.UTF_8)
                + "&access_type=offline"
                + "&prompt=consent";
    }

    @Override
    public Map<String, String> exchangeCodeForUserInfo(String authorizationCode) {
        // Step 1: Exchange authorization code for access token
        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
        tokenParams.add("code", authorizationCode);
        tokenParams.add("client_id", clientId);
        tokenParams.add("client_secret", clientSecret);
        tokenParams.add("redirect_uri", redirectUri);
        tokenParams.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(tokenParams, tokenHeaders);

        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> tokenResponse = restTemplate.exchange(
                GOOGLE_TOKEN_URL,
                HttpMethod.POST,
                tokenRequest,
                (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        Map<String, Object> tokenBody = tokenResponse.getBody();
        if (tokenBody == null || !tokenBody.containsKey("access_token")) {
            throw new RuntimeException("Failed to obtain access token from Google");
        }

        String accessToken = (String) tokenBody.get("access_token");

        // Step 2: Use access token to get user info
        HttpHeaders userInfoHeaders = new HttpHeaders();
        userInfoHeaders.setBearerAuth(accessToken);
        HttpEntity<Void> userInfoRequest = new HttpEntity<>(userInfoHeaders);

        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> userInfoResponse = restTemplate.exchange(
                GOOGLE_USERINFO_URL,
                HttpMethod.GET,
                userInfoRequest,
                (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        Map<String, Object> userInfoBody = userInfoResponse.getBody();
        if (userInfoBody == null) {
            throw new RuntimeException("Failed to obtain user info from Google");
        }

        // Convert to String map
        Map<String, String> result = new HashMap<>();
        result.put("email", getStringValue(userInfoBody, "email"));
        result.put("given_name", getStringValue(userInfoBody, "given_name"));
        result.put("family_name", getStringValue(userInfoBody, "family_name"));
        result.put("sub", getStringValue(userInfoBody, "sub"));
        result.put("picture", getStringValue(userInfoBody, "picture"));

        return result;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}

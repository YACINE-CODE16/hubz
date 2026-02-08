package com.hubz.presentation.controller;

import com.hubz.application.dto.response.AuthResponse;
import com.hubz.application.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final OAuth2Service oAuth2Service;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * GET /api/auth/oauth2/google
     * Redirects the user to Google's OAuth2 authorization page.
     */
    @GetMapping("/google")
    public ResponseEntity<Void> redirectToGoogle() {
        String authorizationUrl = oAuth2Service.getGoogleAuthorizationUrl();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(authorizationUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * GET /api/auth/oauth2/google/callback?code=...
     * Handles the callback from Google after user authorization.
     * Exchanges the code for user info, creates/logs in the user,
     * then redirects to the frontend with the JWT token.
     */
    @GetMapping("/google/callback")
    public ResponseEntity<Void> handleGoogleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error) {

        if (error != null) {
            String redirectUrl = frontendUrl + "/login?oauth_error="
                    + URLEncoder.encode("Google authentication was cancelled or failed", StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(redirectUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

        try {
            AuthResponse authResponse = oAuth2Service.handleGoogleCallback(code);

            // Redirect to frontend with token as query parameter
            String redirectUrl = frontendUrl + "/oauth/callback?token="
                    + URLEncoder.encode(authResponse.getToken(), StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(redirectUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (Exception e) {
            String redirectUrl = frontendUrl + "/login?oauth_error="
                    + URLEncoder.encode("Authentication failed: " + e.getMessage(), StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(redirectUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }
}

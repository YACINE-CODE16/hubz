package com.hubz.application.port.out;

import java.util.Map;

/**
 * Port interface for interacting with Google OAuth2 API.
 * Infrastructure layer provides the implementation that makes actual HTTP calls to Google.
 */
public interface GoogleOAuth2Port {

    /**
     * Builds the Google OAuth2 authorization URL that the user should be redirected to.
     *
     * @return the full Google authorization URL
     */
    String buildAuthorizationUrl();

    /**
     * Exchanges an authorization code for Google user information.
     *
     * @param authorizationCode the code received from Google's OAuth2 callback
     * @return a map containing user info: "email", "given_name", "family_name", "sub" (Google user ID), "picture"
     */
    Map<String, String> exchangeCodeForUserInfo(String authorizationCode);
}

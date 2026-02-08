package com.hubz.presentation.controller;

import com.hubz.application.dto.request.UpdatePreferencesRequest;
import com.hubz.application.dto.response.UserPreferencesResponse;
import com.hubz.application.service.UserPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Set;

/**
 * REST controller for managing user preferences.
 */
@RestController
@RequestMapping("/api/users/me/preferences")
@RequiredArgsConstructor
public class PreferencesController {

    private final UserPreferencesService preferencesService;

    /**
     * Get current user's preferences.
     * If no preferences exist, returns default values.
     *
     * @param principal the authenticated user
     * @return the user preferences
     */
    @GetMapping
    public ResponseEntity<UserPreferencesResponse> getPreferences(Principal principal) {
        UserPreferencesResponse response = preferencesService.getPreferences(principal.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Update current user's preferences.
     *
     * @param principal the authenticated user
     * @param request the update request
     * @return the updated preferences
     */
    @PutMapping
    public ResponseEntity<UserPreferencesResponse> updatePreferences(
            Principal principal,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        UserPreferencesResponse response = preferencesService.updatePreferences(principal.getName(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get list of supported timezones.
     *
     * @return set of timezone strings
     */
    @GetMapping("/timezones")
    public ResponseEntity<Set<String>> getSupportedTimezones() {
        return ResponseEntity.ok(preferencesService.getSupportedTimezones());
    }
}

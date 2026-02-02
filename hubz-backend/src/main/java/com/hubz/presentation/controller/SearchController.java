package com.hubz.presentation.controller;

import com.hubz.application.dto.response.SearchResultResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.SearchService;
import com.hubz.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping
    public ResponseEntity<SearchResultResponse> search(
            @RequestParam(required = false, defaultValue = "") String q,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        SearchResultResponse results = searchService.search(q, userId);
        return ResponseEntity.ok(results);
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}

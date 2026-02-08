package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private UserResponse user;

    /**
     * Indicates that 2FA is required to complete login.
     * When true, the client should prompt for TOTP code and retry login.
     */
    @Builder.Default
    private boolean requires2FA = false;
}

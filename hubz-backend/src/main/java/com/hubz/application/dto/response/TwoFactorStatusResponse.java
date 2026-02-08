package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response containing the 2FA status for a user.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorStatusResponse {

    /**
     * Whether 2FA is enabled for the user.
     */
    private boolean enabled;

    /**
     * Message describing the current status.
     */
    private String message;
}

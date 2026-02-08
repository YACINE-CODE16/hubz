package com.hubz.application.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request for initiating 2FA setup.
 * This is an empty request as the setup only requires authentication.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
public class TwoFactorSetupRequest {
    // Empty request - authentication via JWT is sufficient
}

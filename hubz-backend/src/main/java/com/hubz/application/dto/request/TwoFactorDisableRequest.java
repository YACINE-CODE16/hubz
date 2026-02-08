package com.hubz.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request for disabling 2FA. Requires password and TOTP code for security.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorDisableRequest {

    @NotBlank(message = "Le mot de passe est requis")
    private String password;

    @NotBlank(message = "Le code TOTP est requis")
    @Size(min = 6, max = 6, message = "Le code TOTP doit contenir 6 chiffres")
    private String code;
}

package com.hubz.domain.exception;

public class EmailAlreadyVerifiedException extends RuntimeException {

    public EmailAlreadyVerifiedException() {
        super("Cette adresse email est déjà vérifiée");
    }

    public EmailAlreadyVerifiedException(String email) {
        super("L'adresse email " + email + " est déjà vérifiée");
    }
}

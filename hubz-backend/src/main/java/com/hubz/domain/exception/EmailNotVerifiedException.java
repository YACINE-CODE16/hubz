package com.hubz.domain.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Veuillez vérifier votre adresse email avant de vous connecter");
    }

    public EmailNotVerifiedException(String email) {
        super("L'adresse email " + email + " n'a pas été vérifiée");
    }
}

package dev.rotadev.tracking.application;

/** Registro recusado por uma regra (data futura, duplicidade...). A mensagem vai para o usuário. */
public class InvalidTrackingException extends RuntimeException {

    public InvalidTrackingException(String message) {
        super(message);
    }
}

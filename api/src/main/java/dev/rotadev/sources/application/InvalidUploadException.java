package dev.rotadev.sources.application;

/** Arquivo recusado: formato errado, vazio ou grande demais. A mensagem vai para o usuário. */
public class InvalidUploadException extends RuntimeException {

    public InvalidUploadException(String message) {
        super(message);
    }
}

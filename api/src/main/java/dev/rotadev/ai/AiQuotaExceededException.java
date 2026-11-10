package dev.rotadev.ai;

/** Limite mensal de gerações atingido. Vira 429 (Too Many Requests) com a mensagem para o usuário. */
public class AiQuotaExceededException extends RuntimeException {

    public AiQuotaExceededException(String message) {
        super(message);
    }
}

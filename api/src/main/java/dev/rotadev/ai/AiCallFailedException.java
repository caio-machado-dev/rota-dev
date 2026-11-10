package dev.rotadev.ai;

/**
 * A chamada à IA não produziu uma saída utilizável. Os detalhes ficam em {@code ai_calls}.
 *
 * <p>{@link #retryable()} separa os dois casos para quem chama (normalmente um job): falha de rede ou
 * da API vale tentar de novo mais tarde; recusa, resposta cortada ou JSON inválido, não.
 */
public class AiCallFailedException extends RuntimeException {

    private final boolean retryable;

    public AiCallFailedException(String message) {
        super(message);
        this.retryable = false;
    }

    public AiCallFailedException(String message, Throwable cause) {
        super(message, cause);
        this.retryable = true;
    }

    public boolean retryable() {
        return retryable;
    }
}

package dev.rotadev.ai;

/** A chamada à IA não produziu uma saída utilizável. Os detalhes ficam em {@code ai_calls}. */
public class AiCallFailedException extends RuntimeException {

    public AiCallFailedException(String message) {
        super(message);
    }

    public AiCallFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

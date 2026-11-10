package dev.rotadev.ai;

import java.util.UUID;

/**
 * @param json   saída da IA, já validada contra o schema do prompt
 * @param model  modelo que respondeu
 * @param callId linha em {@code ai_calls} da tentativa que deu certo
 */
public record AiResult(String json, String model, UUID callId) {
}

package dev.rotadev.ai;

import java.util.Objects;
import java.util.UUID;

/**
 * @param prompt    nome da pasta em {@code resources/prompts/}, ex.: {@code "diagnosis"}
 * @param version   versão do prompt ({@code v<N>.md}); mudar o texto do prompt = versão nova
 * @param inputJson dados do usuário que vão na mensagem, em JSON
 */
public record AiRequest(UUID userId, String prompt, int version, String inputJson) {

    public AiRequest {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(prompt, "prompt");
        Objects.requireNonNull(inputJson, "inputJson");
        if (version < 1) {
            throw new IllegalArgumentException("version começa em 1");
        }
    }
}

package dev.rotadev.ai.application;

import dev.rotadev.ai.domain.TokenUsage;

/**
 * A chamada crua ao provedor. A implementação ({@code AnthropicLlmGateway}) só traduz para o SDK;
 * tudo o que é regra (validar, tentar de novo, registrar) fica no {@link CareerAiService}, que os
 * testes exercitam com uma implementação falsa desta interface.
 */
public interface LlmGateway {

    LlmResponse send(String system, String userContent, String schemaJson);

    /**
     * @param stopReason {@code end_turn}, {@code max_tokens}, {@code refusal}...
     */
    record LlmResponse(String text, String stopReason, String model, TokenUsage usage) {
    }
}

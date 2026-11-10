package dev.rotadev.ai.application;

/**
 * Um prompt versionado: o texto de sistema ({@code v<N>.md}) e o JSON Schema da saída
 * ({@code v<N>.schema.json}), lidos de {@code resources/prompts/<nome>/}.
 */
public record Prompt(String name, int version, String system, String schemaJson) {
}

package dev.rotadev.diagnosis.domain;

/**
 * Um eixo do radar: uma competência que o cargo-alvo cobra.
 *
 * @param key      identificador estável, usado no JSON da IA e no front
 * @param required nota exigida pelo cargo-alvo, de 0 a 5
 */
public record Axis(String key, String name, String description, int required) {
}

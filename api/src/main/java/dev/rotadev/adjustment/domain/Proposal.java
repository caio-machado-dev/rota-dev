package dev.rotadev.adjustment.domain;

/**
 * Proposta de reajuste (glossário).
 *
 * @param reason   por que, em linguagem de gente, para a tela
 * @param guidance orientação que vai para a IA se a pessoa aceitar
 */
public record Proposal(AdjustmentKind kind, String reason, String guidance) {
}

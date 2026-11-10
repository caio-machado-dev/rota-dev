package dev.rotadev.diagnosis.domain;

import java.util.List;
import java.util.UUID;

/**
 * Nota de um eixo, com as evidências que a sustentam.
 *
 * @param verified {@code false} quando não há evidência: o eixo aparece como "não verificado"
 */
public record AxisAssessment(String key, String name, int required, int score, boolean verified, List<UUID> evidenceIds, String rationale) {

    public int gap() {
        return Math.max(0, required - score);
    }
}

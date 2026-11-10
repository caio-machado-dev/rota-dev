package dev.rotadev.diagnosis.domain;

import java.util.List;

/**
 * O que a IA devolve, no formato de {@code prompts/diagnosis/v1.schema.json}. Ainda não é confiável:
 * passa pelas {@link DiagnosisRules} antes de virar um {@link DiagnosisResult}.
 */
public record AiDiagnosis(String summary, String estimatedLevel, List<AxisAnswer> axes, List<GapAnswer> gaps) {

    public record AxisAnswer(String key, int score, List<String> evidenceIds, String rationale) {
    }

    public record GapAnswer(String axisKey, String description) {
    }
}

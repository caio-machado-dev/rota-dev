package dev.rotadev.diagnosis.domain;

import java.util.List;

/** Diagnóstico já conferido pelas regras do domínio. É o que vai para o banco e para a tela. */
public record DiagnosisResult(String summary, String estimatedLevel, List<AxisAssessment> axes, List<Gap> gaps) {
}

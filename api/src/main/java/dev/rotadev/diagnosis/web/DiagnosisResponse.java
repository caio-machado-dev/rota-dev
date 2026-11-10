package dev.rotadev.diagnosis.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Diagnóstico para a tela. Cada eixo traz as evidências citadas já com título e link, para o front
 * mostrar a prova ao lado da nota.
 */
public record DiagnosisResponse(
        @NotNull UUID id,
        @NotNull String status,
        @NotNull Instant createdAt,
        String summary,
        String estimatedLevel,
        @NotNull List<AxisResponse> axes,
        @NotNull List<GapResponse> gaps,
        String error) {

    public record AxisResponse(
            @NotNull String key, @NotNull String name, @NotNull int required, @NotNull int score, @NotNull boolean verified,
            String rationale, @NotNull List<CitedEvidence> evidences) {
    }

    /** {@code title} nulo quando a evidência é de um snapshot que já foi substituído. */
    public record CitedEvidence(@NotNull UUID id, String title, String url) {
    }

    public record GapResponse(@NotNull String axisKey, @NotNull String axisName, @NotNull int size, @NotNull int priority, @NotNull String description) {
    }
}

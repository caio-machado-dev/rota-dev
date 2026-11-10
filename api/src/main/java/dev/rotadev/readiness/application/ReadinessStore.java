package dev.rotadev.readiness.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import dev.rotadev.readiness.domain.ReadinessScore;

public interface ReadinessStore {

    /** Grava ou substitui o índice da semana (um check-in refeito atualiza o ponto). */
    void upsert(UUID userId, LocalDate weekStart, ReadinessScore score, Instant now);

    List<Point> history(UUID userId);

    record Point(LocalDate weekStart, ReadinessScore score) {
    }
}

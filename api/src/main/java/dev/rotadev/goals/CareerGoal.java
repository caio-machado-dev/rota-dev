package dev.rotadev.goals;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Objetivo de carreira (glossário): cargo-alvo, prazo, horas por semana e preferências.
 *
 * <p>{@link LocalDate} (data sem hora nem fuso) porque o prazo é um dia do calendário do usuário.
 * As regras que não dependem de "hoje" ficam aqui no construtor; o prazo no futuro é conferido no
 * caso de uso, que conhece o relógio e o fuso.
 */
public record CareerGoal(
        UUID userId,
        String targetRole,
        Level targetLevel,
        Track track,
        LocalDate deadline,
        int hoursPerWeek,
        String primaryStack,
        boolean wantsCertifications,
        String notes) {

    public static final int MAX_HOURS_PER_WEEK = 60;

    public CareerGoal {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(targetLevel, "targetLevel");
        Objects.requireNonNull(track, "track");
        Objects.requireNonNull(deadline, "deadline");
        if (targetRole == null || targetRole.isBlank()) {
            throw new IllegalArgumentException("Informe o cargo-alvo.");
        }
        if (hoursPerWeek < 1 || hoursPerWeek > MAX_HOURS_PER_WEEK) {
            throw new IllegalArgumentException("Horas por semana devem estar entre 1 e " + MAX_HOURS_PER_WEEK + ".");
        }
    }
}

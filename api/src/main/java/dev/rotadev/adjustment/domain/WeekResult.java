package dev.rotadev.adjustment.domain;

import java.time.LocalDate;

/** O que um check-in disse sobre a semana, no formato que as regras de reajuste usam. */
public record WeekResult(LocalDate weekStart, int minutesLogged, int reviewDays, int energy, String blockers) {
}

package dev.rotadev.tracking.domain;

import java.time.LocalDate;

/**
 * Check-in semanal (glossário).
 *
 * @param minutesLogged soma das sessões da semana, calculada pelo sistema
 * @param reviewDays    dias da semana com o protocolo anti vibe coding completo, calculado pelo sistema
 * @param commits       informado pela pessoa
 * @param energy        de 1 a 5
 */
public record WeeklyCheckin(LocalDate weekStart, int minutesLogged, int reviewDays, int commits, int energy, String blockers, String notes) {

    public WeeklyCheckin {
        if (commits < 0) {
            throw new IllegalArgumentException("Commits não pode ser negativo.");
        }
        if (energy < 1 || energy > 5) {
            throw new IllegalArgumentException("Energia vai de 1 a 5.");
        }
    }
}

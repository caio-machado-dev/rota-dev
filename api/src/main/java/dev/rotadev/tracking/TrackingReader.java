package dev.rotadev.tracking;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** API pública de leitura do acompanhamento, para a prontidão e os reajustes. */
public interface TrackingReader {

    /** Minutos de sessões com {@code from <= dia <= to}. */
    int minutesBetween(UUID userId, LocalDate from, LocalDate to);

    /** Dias com o protocolo anti vibe coding completo no período. */
    int completeReviewDaysBetween(UUID userId, LocalDate from, LocalDate to);

    /** Quantos portões a pessoa já declarou alcançados. */
    int milestonesAchieved(UUID userId);

    /** Os {@code limit} check-ins mais recentes, do mais novo ao mais antigo. */
    List<CheckinSummary> recentCheckins(UUID userId, int limit);

    record CheckinSummary(LocalDate weekStart, int minutesLogged, int reviewDays, int energy, String blockers) {
    }

    /** "Hoje" no fuso do usuário. */
    LocalDate today(UUID userId);
}

package dev.rotadev.tracking;

import java.time.LocalDate;
import java.util.UUID;

/** API pública de leitura do acompanhamento, para a prontidão e os reajustes. */
public interface TrackingReader {

    /** Minutos de sessões com {@code from <= dia <= to}. */
    int minutesBetween(UUID userId, LocalDate from, LocalDate to);

    /** Dias com o protocolo anti vibe coding completo no período. */
    int completeReviewDaysBetween(UUID userId, LocalDate from, LocalDate to);

    /** Quantos portões a pessoa já declarou alcançados. */
    int milestonesAchieved(UUID userId);

    /** "Hoje" no fuso do usuário. */
    LocalDate today(UUID userId);
}

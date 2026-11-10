package dev.rotadev.tracking.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

/**
 * Qual semana pode receber check-in hoje. O check-in é de domingo, mas a vida acontece:
 * <ul>
 *   <li>sábado e domingo: a semana atual;</li>
 *   <li>segunda e terça: a semana anterior, para quem esqueceu no domingo;</li>
 *   <li>quarta a sexta: nenhuma (a semana ainda está no meio).</li>
 * </ul>
 */
public final class CheckinWindow {

    private CheckinWindow() {
    }

    /** @return a segunda-feira da semana que pode receber check-in, se houver */
    public static Optional<LocalDate> weekFor(LocalDate today) {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return switch (today.getDayOfWeek()) {
            case SATURDAY, SUNDAY -> Optional.of(monday);
            case MONDAY, TUESDAY -> Optional.of(monday.minusWeeks(1));
            default -> Optional.empty();
        };
    }
}

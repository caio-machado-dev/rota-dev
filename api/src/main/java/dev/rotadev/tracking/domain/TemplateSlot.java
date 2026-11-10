package dev.rotadev.tracking.domain;

import java.time.DayOfWeek;

/** Uma entrada da semana padrão: tantos minutos de tal atividade em tal dia da semana. */
public record TemplateSlot(DayOfWeek weekday, ActivityKind kind, int minutes) {

    public TemplateSlot {
        if (minutes < StudySession.MIN_MINUTES || minutes > StudySession.MAX_MINUTES) {
            throw new IllegalArgumentException("Minutos fora do intervalo.");
        }
    }
}

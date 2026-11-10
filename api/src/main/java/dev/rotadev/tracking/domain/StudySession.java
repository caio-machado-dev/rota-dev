package dev.rotadev.tracking.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Sessão de estudo (glossário).
 *
 * @param learning uma linha do que aprendeu; opcional, mas é o que transforma tempo em aprendizado
 */
public record StudySession(UUID id, LocalDate day, ActivityKind kind, int minutes, String topic, String learning) {

    public static final int MIN_MINUTES = 5;
    public static final int MAX_MINUTES = 600;

    public StudySession {
        if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) {
            throw new IllegalArgumentException("Uma sessão tem entre " + MIN_MINUTES + " e " + MAX_MINUTES + " minutos.");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Informe o tema da sessão.");
        }
    }
}

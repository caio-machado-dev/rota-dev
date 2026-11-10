package dev.rotadev.jobs.domain;

import java.time.Duration;

/**
 * Espera entre tentativas: dobra a cada falha (exponencial) e para num teto.
 * Com base de 30 s: 30 s, 1 min, 2 min, 4 min... até {@code max}.
 */
public record Backoff(Duration base, Duration max) {

    public Backoff {
        if (base.isNegative() || base.isZero() || max.compareTo(base) < 0) {
            throw new IllegalArgumentException("base deve ser positiva e max >= base");
        }
    }

    /** Espera depois da tentativa {@code attempt} (1 = primeira) ter falhado. */
    public Duration delayAfter(int attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt começa em 1");
        }
        // Limita o expoente para o deslocamento de bits não estourar o long.
        int exponent = Math.min(attempt - 1, 30);
        Duration delay = base.multipliedBy(1L << exponent);
        return delay.compareTo(max) > 0 ? max : delay;
    }
}

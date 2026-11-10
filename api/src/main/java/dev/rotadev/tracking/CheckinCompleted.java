package dev.rotadev.tracking;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento: alguém fechou o check-in de uma semana. O módulo {@code readiness} escuta para gravar o
 * índice de prontidão daquele momento, sem que o {@code tracking} precise conhecê-lo (evita ciclo).
 */
public record CheckinCompleted(UUID userId, LocalDate weekStart) {
}

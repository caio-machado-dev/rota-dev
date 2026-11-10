package dev.rotadev.sources.web;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** Uma fonte conectada, sem credencial nenhuma. */
public record SourceResponse(
        @NotNull UUID id,
        @NotNull String type,
        String externalLogin,
        @NotNull Instant connectedAt,
        Instant lastSyncedAt) {
}

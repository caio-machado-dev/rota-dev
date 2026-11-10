package dev.rotadev.ai.application;

import java.time.Instant;
import java.util.UUID;

import dev.rotadev.ai.domain.TokenUsage;

/** Grava uma linha em {@code ai_calls} por tentativa. */
public interface AiCallLog {

    UUID record(Entry entry);

    record Entry(
            UUID userId, String promptName, int promptVersion, String model, int attempt, String status,
            TokenUsage usage, Long costUsdMicros, long latencyMs, String error, Instant createdAt) {
    }
}

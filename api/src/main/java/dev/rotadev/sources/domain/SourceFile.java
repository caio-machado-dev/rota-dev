package dev.rotadev.sources.domain;

import java.time.Instant;
import java.util.UUID;

/** Metadados de um arquivo enviado. O conteúdo fica no {@code FileStorage}. */
public record SourceFile(
        UUID id, UUID userId, UploadKind kind, DocumentFormat format, String originalName, long sizeBytes,
        String sha256, String storageKey, String status, Instant createdAt) {
}

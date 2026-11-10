package dev.rotadev.sources.web;

import java.time.Instant;
import java.util.UUID;

import dev.rotadev.sources.domain.SourceFile;
import jakarta.validation.constraints.NotNull;

/** Um arquivo enviado, sem o caminho no disco nem o hash. */
public record UploadResponse(
        @NotNull UUID id, @NotNull String kind, @NotNull String format, @NotNull String originalName,
        long sizeBytes, @NotNull String status, @NotNull Instant createdAt) {

    static UploadResponse from(SourceFile f) {
        return new UploadResponse(f.id(), f.kind().name(), f.format().name(), f.originalName(), f.sizeBytes(), f.status(), f.createdAt());
    }
}

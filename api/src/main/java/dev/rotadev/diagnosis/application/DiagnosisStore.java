package dev.rotadev.diagnosis.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import dev.rotadev.diagnosis.domain.DiagnosisResult;

public interface DiagnosisStore {

    UUID insertPending(UUID userId, String goalJson, Instant now);

    void markReady(UUID userId, UUID id, DiagnosisResult result, UUID aiCallId, Instant now);

    void markFailed(UUID userId, UUID id, String error, Instant now);

    Optional<StoredDiagnosis> find(UUID userId, UUID id);

    Optional<StoredDiagnosis> latest(UUID userId);

    /** Diagnóstico como está no banco. {@code result} é nulo enquanto PENDING ou se FAILED. */
    record StoredDiagnosis(UUID id, String status, String goalJson, DiagnosisResult result, String error, Instant createdAt) {
    }
}

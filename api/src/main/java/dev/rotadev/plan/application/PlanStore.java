package dev.rotadev.plan.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.rotadev.plan.domain.PlanContent;

public interface PlanStore {

    int nextVersion(UUID userId);

    UUID insertPending(UUID userId, int version, String reason, UUID diagnosisId, LocalDate startsOn, String guidance, Instant now);

    void markReady(UUID userId, UUID id, PlanContent content, UUID aiCallId, Instant now);

    void markFailed(UUID userId, UUID id, String error, Instant now);

    Optional<StoredPlan> find(UUID userId, UUID id);

    /** A versão mais recente, em qualquer status. */
    Optional<StoredPlan> latest(UUID userId);

    /** A versão pronta mais recente: é o plano em vigor. */
    Optional<StoredPlan> current(UUID userId);

    List<StoredPlan> history(UUID userId);

    /** @param guidance orientação extra para a IA (reajuste), ou nula */
    record StoredPlan(UUID id, int version, String status, String reason, UUID diagnosisId, LocalDate startsOn,
            PlanContent content, String error, Instant createdAt, String guidance) {
    }
}

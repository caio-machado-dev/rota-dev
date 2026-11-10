package dev.rotadev.adjustment.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.rotadev.adjustment.domain.Proposal;

public interface AdjustmentStore {

    /** @return {@code false} se já havia proposta para a semana */
    boolean insertProposal(UUID userId, LocalDate weekStart, Proposal proposal, Instant now);

    Optional<StoredAdjustment> find(UUID userId, UUID id);

    List<StoredAdjustment> list(UUID userId);

    void decide(UUID userId, UUID id, String status, UUID planVersionId, Instant now);

    record StoredAdjustment(UUID id, LocalDate weekStart, Proposal proposal, String status, UUID planVersionId, Instant createdAt) {
    }
}

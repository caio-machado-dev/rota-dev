package dev.rotadev.profile.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.rotadev.profile.Evidence;
import dev.rotadev.profile.SkillSignal;
import dev.rotadev.profile.Snapshot;

/** Persistência de snapshots e evidências. Toda leitura filtra por {@code userId} (ADR 0004). */
public interface SnapshotStore {

    boolean existsForJob(UUID jobId);

    UUID insertSnapshot(UUID userId, String sourceType, UUID jobId, String rawJson, List<SkillSignal> skills, Instant now);

    void insertEvidences(UUID userId, UUID snapshotId, List<Evidence> evidences, Instant now);

    List<Snapshot> latestPerSource(UUID userId);

    Optional<Snapshot> find(UUID userId, UUID snapshotId);
}

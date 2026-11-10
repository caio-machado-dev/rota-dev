package dev.rotadev.profile.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.profile.Evidence;
import dev.rotadev.profile.ProfileReader;
import dev.rotadev.profile.ProfileRecorder;
import dev.rotadev.profile.SkillSignal;
import dev.rotadev.profile.Snapshot;

/**
 * Implementa as duas interfaces públicas do módulo. Os outros módulos só enxergam as interfaces
 * ({@link ProfileRecorder}, {@link ProfileReader}); esta classe, num subpacote, é interna.
 */
@Service
class ProfileService implements ProfileRecorder, ProfileReader {

    private final SnapshotStore store;
    private final Clock clock;

    ProfileService(SnapshotStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    /** Snapshot e evidências na mesma transação: ou grava tudo, ou nada. */
    @Override
    @Transactional
    public boolean record(UUID userId, String sourceType, UUID jobId, String rawJson, List<SkillSignal> skills, List<Evidence> evidences) {
        if (jobId != null && store.existsForJob(jobId)) {
            return false;
        }
        Instant now = clock.instant();
        UUID snapshotId = store.insertSnapshot(userId, sourceType, jobId, rawJson, skills, now);
        store.insertEvidences(userId, snapshotId, evidences, now);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Snapshot> latestPerSource(UUID userId) {
        return store.latestPerSource(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Snapshot> find(UUID userId, UUID snapshotId) {
        return store.find(userId, snapshotId);
    }
}

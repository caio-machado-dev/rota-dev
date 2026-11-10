package dev.rotadev.profile.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.rotadev.profile.Evidence;
import dev.rotadev.profile.SkillSignal;
import dev.rotadev.profile.Snapshot;
import jakarta.validation.constraints.NotNull;

/**
 * Records de resposta do perfil. Ficam juntos num arquivo porque só existem um para o outro;
 * {@code static} aninhado em Java é como uma classe definida dentro de outra em Python.
 */
final class ProfileResponses {

    private ProfileResponses() {
    }

    record ProfileResponse(@NotNull List<SnapshotResponse> snapshots) {
    }

    record SnapshotResponse(
            @NotNull UUID id,
            @NotNull String sourceType,
            @NotNull Instant createdAt,
            @NotNull List<SkillResponse> skills,
            @NotNull List<EvidenceResponse> evidences) {

        static SnapshotResponse from(Snapshot s) {
            return new SnapshotResponse(s.id(), s.sourceType(), s.createdAt(),
                    s.skills().stream().map(SkillResponse::from).toList(),
                    s.evidences().stream().map(EvidenceResponse::from).toList());
        }
    }

    record SkillResponse(@NotNull String name, double weight, @NotNull String confidence) {

        static SkillResponse from(SkillSignal s) {
            return new SkillResponse(s.name(), s.weight(), s.confidence().name());
        }
    }

    record EvidenceResponse(
            @NotNull UUID id, @NotNull String kind, String skill, @NotNull String title, String detail, String url,
            @NotNull String confidence) {

        static EvidenceResponse from(Evidence e) {
            return new EvidenceResponse(e.id(), e.kind(), e.skill(), e.title(), e.detail(), e.url(), e.confidence().name());
        }
    }
}

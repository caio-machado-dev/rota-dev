package dev.rotadev.profile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Um snapshot de perfil já gravado, com as habilidades e as evidências dele. */
public record Snapshot(UUID id, String sourceType, Instant createdAt, List<SkillSignal> skills, List<Evidence> evidences) {
}

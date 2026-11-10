package dev.rotadev.profile.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import dev.rotadev.profile.Confidence;
import dev.rotadev.profile.Evidence;
import dev.rotadev.profile.SkillSignal;
import dev.rotadev.profile.Snapshot;
import dev.rotadev.profile.application.SnapshotStore;
import dev.rotadev.shared.UuidV7;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Snapshots em SQL direto. As colunas JSONB entram com {@code CAST(:x AS jsonb)} e saem como texto
 * ({@code ::text}), convertido com o Jackson. O {@link JsonMapper} é o "json.dumps/json.loads" do Java;
 * o Spring Boot já cria um configurado.
 */
@Repository
class JdbcSnapshotStore implements SnapshotStore {

    private static final TypeReference<List<SkillSignal>> SKILL_LIST = new TypeReference<>() { };

    private final JdbcClient jdbc;
    private final JsonMapper json;

    JdbcSnapshotStore(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public boolean existsForJob(UUID jobId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM profile_snapshots WHERE job_id = :jobId)")
                .param("jobId", jobId).query(Boolean.class).single();
    }

    @Override
    public UUID insertSnapshot(UUID userId, String sourceType, UUID jobId, String rawJson, List<SkillSignal> skills, Instant now) {
        UUID id = UuidV7.generate(now);
        jdbc.sql("""
                INSERT INTO profile_snapshots (id, user_id, source_type, job_id, raw, skills, created_at)
                VALUES (:id, :userId, :sourceType, :jobId, CAST(:raw AS jsonb), CAST(:skills AS jsonb), :now)
                """)
                .param("id", id)
                .param("userId", userId)
                .param("sourceType", sourceType)
                .param("jobId", jobId)
                .param("raw", rawJson)
                .param("skills", json.writeValueAsString(skills))
                .param("now", Timestamp.from(now))
                .update();
        return id;
    }

    @Override
    public void insertEvidences(UUID userId, UUID snapshotId, List<Evidence> evidences, Instant now) {
        for (Evidence e : evidences) {
            jdbc.sql("""
                    INSERT INTO evidences (id, user_id, snapshot_id, kind, skill, title, detail, url, confidence, created_at)
                    VALUES (:id, :userId, :snapshotId, :kind, :skill, :title, :detail, :url, :confidence, :now)
                    """)
                    .param("id", UuidV7.generate(now))
                    .param("userId", userId)
                    .param("snapshotId", snapshotId)
                    .param("kind", e.kind())
                    .param("skill", e.skill())
                    .param("title", e.title())
                    .param("detail", e.detail())
                    .param("url", e.url())
                    .param("confidence", e.confidence().name())
                    .param("now", Timestamp.from(now))
                    .update();
        }
    }

    /** {@code DISTINCT ON} é do Postgres: fica com a primeira linha de cada fonte, na ordem do ORDER BY. */
    @Override
    public List<Snapshot> latestPerSource(UUID userId) {
        List<SnapshotRow> rows = jdbc.sql("""
                SELECT DISTINCT ON (source_type) id, source_type, created_at, skills::text AS skills
                  FROM profile_snapshots
                 WHERE user_id = :userId
                 ORDER BY source_type, created_at DESC
                """)
                .param("userId", userId)
                .query(this::mapRow)
                .list();
        return withEvidences(userId, rows);
    }

    @Override
    public Optional<Snapshot> find(UUID userId, UUID snapshotId) {
        // O filtro por user_id é o que impede ler o snapshot de outra pessoa (ADR 0004).
        List<SnapshotRow> rows = jdbc.sql("""
                SELECT id, source_type, created_at, skills::text AS skills
                  FROM profile_snapshots
                 WHERE id = :id AND user_id = :userId
                """)
                .param("id", snapshotId)
                .param("userId", userId)
                .query(this::mapRow)
                .list();
        return withEvidences(userId, rows).stream().findFirst();
    }

    private List<Snapshot> withEvidences(UUID userId, List<SnapshotRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<Evidence>> bySnapshot = jdbc.sql("""
                SELECT id, snapshot_id, kind, skill, title, detail, url, confidence
                  FROM evidences
                 WHERE user_id = :userId AND snapshot_id IN (:ids)
                 ORDER BY created_at, id
                """)
                .param("userId", userId)
                .param("ids", rows.stream().map(SnapshotRow::id).toList())
                .query((rs, n) -> Map.entry(rs.getObject("snapshot_id", UUID.class), new Evidence(
                        rs.getObject("id", UUID.class),
                        rs.getString("kind"),
                        rs.getString("skill"),
                        rs.getString("title"),
                        rs.getString("detail"),
                        rs.getString("url"),
                        Confidence.valueOf(rs.getString("confidence")))))
                .list().stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        return rows.stream()
                .map(r -> new Snapshot(r.id(), r.sourceType(), r.createdAt(), r.skills(), bySnapshot.getOrDefault(r.id(), List.of())))
                .toList();
    }

    private SnapshotRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SnapshotRow(
                rs.getObject("id", UUID.class),
                rs.getString("source_type"),
                rs.getTimestamp("created_at").toInstant(),
                json.readValue(rs.getString("skills"), SKILL_LIST));
    }

    private record SnapshotRow(UUID id, String sourceType, Instant createdAt, List<SkillSignal> skills) {
    }
}

package dev.rotadev.plan.infrastructure;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import dev.rotadev.plan.application.PlanStore;
import dev.rotadev.plan.domain.PlanContent;
import dev.rotadev.shared.UuidV7;
import tools.jackson.databind.json.JsonMapper;

@Repository
class JdbcPlanStore implements PlanStore {

    private static final String COLUMNS =
            "id, version, status, reason, diagnosis_id, starts_on, content::text AS content, error, created_at, guidance";

    private final JdbcClient jdbc;
    private final JsonMapper json;

    JdbcPlanStore(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /**
     * Dois pedidos simultâneos podem calcular o mesmo número; o {@code UNIQUE (user_id, version)} faz
     * o segundo falhar em vez de gravar duas "versão 2".
     */
    @Override
    public int nextVersion(UUID userId) {
        return jdbc.sql("SELECT COALESCE(MAX(version), 0) + 1 FROM plan_versions WHERE user_id = :userId")
                .param("userId", userId).query(Integer.class).single();
    }

    @Override
    public UUID insertPending(UUID userId, int version, String reason, UUID diagnosisId, LocalDate startsOn, String guidance, Instant now) {
        UUID id = UuidV7.generate(now);
        jdbc.sql("""
                INSERT INTO plan_versions (id, user_id, version, status, reason, diagnosis_id, starts_on, guidance, created_at)
                VALUES (:id, :userId, :version, 'PENDING', :reason, :diagnosisId, :startsOn, :guidance, :now)
                """)
                .param("id", id).param("userId", userId).param("version", version).param("reason", reason)
                .param("diagnosisId", diagnosisId).param("startsOn", Date.valueOf(startsOn)).param("guidance", guidance)
                .param("now", Timestamp.from(now))
                .update();
        return id;
    }

    @Override
    public void markReady(UUID userId, UUID id, PlanContent content, UUID aiCallId, Instant now) {
        jdbc.sql("""
                UPDATE plan_versions SET status = 'READY', content = CAST(:content AS jsonb), ai_call_id = :callId, finished_at = :now
                 WHERE id = :id AND user_id = :userId
                """)
                .param("content", json.writeValueAsString(content)).param("callId", aiCallId).param("now", Timestamp.from(now))
                .param("id", id).param("userId", userId)
                .update();
    }

    @Override
    public void markFailed(UUID userId, UUID id, String error, Instant now) {
        jdbc.sql("UPDATE plan_versions SET status = 'FAILED', error = :error, finished_at = :now WHERE id = :id AND user_id = :userId")
                .param("error", error).param("now", Timestamp.from(now)).param("id", id).param("userId", userId)
                .update();
    }

    @Override
    public Optional<StoredPlan> find(UUID userId, UUID id) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM plan_versions WHERE id = :id AND user_id = :userId")
                .param("id", id).param("userId", userId).query(this::map).optional();
    }

    @Override
    public Optional<StoredPlan> latest(UUID userId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM plan_versions WHERE user_id = :userId ORDER BY version DESC LIMIT 1")
                .param("userId", userId).query(this::map).optional();
    }

    @Override
    public Optional<StoredPlan> current(UUID userId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM plan_versions WHERE user_id = :userId AND status = 'READY' ORDER BY version DESC LIMIT 1")
                .param("userId", userId).query(this::map).optional();
    }

    @Override
    public List<StoredPlan> history(UUID userId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM plan_versions WHERE user_id = :userId ORDER BY version DESC")
                .param("userId", userId).query(this::map).list();
    }

    private StoredPlan map(ResultSet rs, int row) throws SQLException {
        String content = rs.getString("content");
        return new StoredPlan(
                rs.getObject("id", UUID.class),
                rs.getInt("version"),
                rs.getString("status"),
                rs.getString("reason"),
                rs.getObject("diagnosis_id", UUID.class),
                rs.getDate("starts_on").toLocalDate(),
                content == null ? null : json.readValue(content, PlanContent.class),
                rs.getString("error"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getString("guidance"));
    }
}

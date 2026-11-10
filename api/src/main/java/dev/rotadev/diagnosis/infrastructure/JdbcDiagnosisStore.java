package dev.rotadev.diagnosis.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import dev.rotadev.diagnosis.application.DiagnosisStore;
import dev.rotadev.diagnosis.domain.DiagnosisResult;
import dev.rotadev.shared.UuidV7;
import tools.jackson.databind.json.JsonMapper;

@Repository
class JdbcDiagnosisStore implements DiagnosisStore {

    private static final String COLUMNS = "id, status, goal::text AS goal, result::text AS result, error, created_at";

    private final JdbcClient jdbc;
    private final JsonMapper json;

    JdbcDiagnosisStore(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public UUID insertPending(UUID userId, String goalJson, Instant now) {
        UUID id = UuidV7.generate(now);
        jdbc.sql("INSERT INTO diagnoses (id, user_id, status, goal, created_at) VALUES (:id, :userId, 'PENDING', CAST(:goal AS jsonb), :now)")
                .param("id", id).param("userId", userId).param("goal", goalJson).param("now", Timestamp.from(now))
                .update();
        return id;
    }

    @Override
    public void markReady(UUID userId, UUID id, DiagnosisResult result, UUID aiCallId, Instant now) {
        jdbc.sql("""
                UPDATE diagnoses SET status = 'READY', result = CAST(:result AS jsonb), ai_call_id = :callId, finished_at = :now
                 WHERE id = :id AND user_id = :userId
                """)
                .param("result", json.writeValueAsString(result)).param("callId", aiCallId).param("now", Timestamp.from(now))
                .param("id", id).param("userId", userId)
                .update();
    }

    @Override
    public void markFailed(UUID userId, UUID id, String error, Instant now) {
        jdbc.sql("UPDATE diagnoses SET status = 'FAILED', error = :error, finished_at = :now WHERE id = :id AND user_id = :userId")
                .param("error", error).param("now", Timestamp.from(now)).param("id", id).param("userId", userId)
                .update();
    }

    @Override
    public Optional<StoredDiagnosis> find(UUID userId, UUID id) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM diagnoses WHERE id = :id AND user_id = :userId")
                .param("id", id).param("userId", userId)
                .query(this::map).optional();
    }

    @Override
    public Optional<StoredDiagnosis> latest(UUID userId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM diagnoses WHERE user_id = :userId ORDER BY created_at DESC LIMIT 1")
                .param("userId", userId)
                .query(this::map).optional();
    }

    @Override
    public Optional<StoredDiagnosis> latestReady(UUID userId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM diagnoses WHERE user_id = :userId AND status = 'READY' ORDER BY created_at DESC LIMIT 1")
                .param("userId", userId)
                .query(this::map).optional();
    }

    private StoredDiagnosis map(ResultSet rs, int row) throws SQLException {
        String result = rs.getString("result");
        return new StoredDiagnosis(
                rs.getObject("id", UUID.class),
                rs.getString("status"),
                rs.getString("goal"),
                result == null ? null : json.readValue(result, DiagnosisResult.class),
                rs.getString("error"),
                rs.getTimestamp("created_at").toInstant());
    }
}

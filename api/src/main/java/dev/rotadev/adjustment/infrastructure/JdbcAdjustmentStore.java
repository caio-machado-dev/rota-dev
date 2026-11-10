package dev.rotadev.adjustment.infrastructure;

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

import dev.rotadev.adjustment.application.AdjustmentStore;
import dev.rotadev.adjustment.domain.AdjustmentKind;
import dev.rotadev.adjustment.domain.Proposal;
import dev.rotadev.shared.UuidV7;

@Repository
class JdbcAdjustmentStore implements AdjustmentStore {

    private static final String COLUMNS = "id, week_start, kind, reason, guidance, status, plan_version_id, created_at";

    private final JdbcClient jdbc;

    JdbcAdjustmentStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean insertProposal(UUID userId, LocalDate weekStart, Proposal p, Instant now) {
        return jdbc.sql("""
                INSERT INTO plan_adjustments (id, user_id, week_start, kind, reason, guidance, status, created_at)
                VALUES (:id, :userId, :week, :kind, :reason, :guidance, 'PROPOSED', :now)
                ON CONFLICT (user_id, week_start) DO NOTHING
                """)
                .param("id", UuidV7.generate(now)).param("userId", userId).param("week", Date.valueOf(weekStart))
                .param("kind", p.kind().name()).param("reason", p.reason()).param("guidance", p.guidance())
                .param("now", Timestamp.from(now))
                .update() == 1;
    }

    @Override
    public Optional<StoredAdjustment> find(UUID userId, UUID id) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM plan_adjustments WHERE id = :id AND user_id = :userId")
                .param("id", id).param("userId", userId).query(JdbcAdjustmentStore::map).optional();
    }

    @Override
    public List<StoredAdjustment> list(UUID userId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM plan_adjustments WHERE user_id = :userId ORDER BY week_start DESC")
                .param("userId", userId).query(JdbcAdjustmentStore::map).list();
    }

    @Override
    public void decide(UUID userId, UUID id, String status, UUID planVersionId, Instant now) {
        jdbc.sql("""
                UPDATE plan_adjustments SET status = :status, plan_version_id = :planVersion, decided_at = :now
                 WHERE id = :id AND user_id = :userId AND status = 'PROPOSED'
                """)
                .param("status", status).param("planVersion", planVersionId).param("now", Timestamp.from(now))
                .param("id", id).param("userId", userId)
                .update();
    }

    private static StoredAdjustment map(ResultSet rs, int row) throws SQLException {
        return new StoredAdjustment(
                rs.getObject("id", UUID.class),
                rs.getDate("week_start").toLocalDate(),
                new Proposal(AdjustmentKind.valueOf(rs.getString("kind")), rs.getString("reason"), rs.getString("guidance")),
                rs.getString("status"),
                rs.getObject("plan_version_id", UUID.class),
                rs.getTimestamp("created_at").toInstant());
    }
}

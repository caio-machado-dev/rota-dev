package dev.rotadev.readiness.infrastructure;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import dev.rotadev.readiness.application.ReadinessStore;
import dev.rotadev.readiness.domain.ReadinessScore;

@Repository
class JdbcReadinessStore implements ReadinessStore {

    private final JdbcClient jdbc;

    JdbcReadinessStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsert(UUID userId, LocalDate weekStart, ReadinessScore s, Instant now) {
        jdbc.sql("""
                INSERT INTO readiness_scores (user_id, week_start, score, skills, gates, study, habit, created_at)
                VALUES (:userId, :week, :score, :skills, :gates, :study, :habit, :now)
                ON CONFLICT (user_id, week_start) DO UPDATE SET
                    score = EXCLUDED.score, skills = EXCLUDED.skills, gates = EXCLUDED.gates,
                    study = EXCLUDED.study, habit = EXCLUDED.habit, created_at = EXCLUDED.created_at
                """)
                .param("userId", userId).param("week", Date.valueOf(weekStart)).param("score", s.score()).param("skills", s.skills())
                .param("gates", s.gates()).param("study", s.study()).param("habit", s.habit()).param("now", Timestamp.from(now))
                .update();
    }

    @Override
    public List<Point> history(UUID userId) {
        return jdbc.sql("SELECT * FROM readiness_scores WHERE user_id = :userId ORDER BY week_start")
                .param("userId", userId)
                .query((rs, n) -> new Point(rs.getDate("week_start").toLocalDate(), new ReadinessScore(rs.getInt("score"),
                        rs.getInt("skills"), rs.getInt("gates"), rs.getInt("study"), rs.getInt("habit"))))
                .list();
    }
}

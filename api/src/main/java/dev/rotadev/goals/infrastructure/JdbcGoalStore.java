package dev.rotadev.goals.infrastructure;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import dev.rotadev.goals.CareerGoal;
import dev.rotadev.goals.Level;
import dev.rotadev.goals.Track;
import dev.rotadev.goals.application.GoalStore;

@Repository
class JdbcGoalStore implements GoalStore {

    private final JdbcClient jdbc;

    JdbcGoalStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** {@code ON CONFLICT ... DO UPDATE} é o "upsert" do Postgres: insere ou, se já existir, atualiza. */
    @Override
    public void upsert(CareerGoal g, Instant now) {
        jdbc.sql("""
                INSERT INTO career_goals (user_id, target_role, target_level, track, deadline, hours_per_week,
                                          primary_stack, wants_certifications, notes, created_at, updated_at)
                VALUES (:userId, :role, :level, :track, :deadline, :hours, :stack, :certs, :notes, :now, :now)
                ON CONFLICT (user_id) DO UPDATE SET
                    target_role = EXCLUDED.target_role, target_level = EXCLUDED.target_level, track = EXCLUDED.track,
                    deadline = EXCLUDED.deadline, hours_per_week = EXCLUDED.hours_per_week,
                    primary_stack = EXCLUDED.primary_stack, wants_certifications = EXCLUDED.wants_certifications,
                    notes = EXCLUDED.notes, updated_at = EXCLUDED.updated_at
                """)
                .param("userId", g.userId())
                .param("role", g.targetRole())
                .param("level", g.targetLevel().name())
                .param("track", g.track().name())
                .param("deadline", Date.valueOf(g.deadline()))
                .param("hours", g.hoursPerWeek())
                .param("stack", g.primaryStack())
                .param("certs", g.wantsCertifications())
                .param("notes", g.notes())
                .param("now", Timestamp.from(now))
                .update();
    }

    @Override
    public Optional<CareerGoal> find(UUID userId) {
        return jdbc.sql("SELECT * FROM career_goals WHERE user_id = :userId")
                .param("userId", userId)
                .query((rs, n) -> new CareerGoal(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("target_role"),
                        Level.valueOf(rs.getString("target_level")),
                        Track.valueOf(rs.getString("track")),
                        rs.getDate("deadline").toLocalDate(),
                        rs.getInt("hours_per_week"),
                        rs.getString("primary_stack"),
                        rs.getBoolean("wants_certifications"),
                        rs.getString("notes")))
                .optional();
    }
}

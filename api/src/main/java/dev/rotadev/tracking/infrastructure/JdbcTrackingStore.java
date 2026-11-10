package dev.rotadev.tracking.infrastructure;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import dev.rotadev.tracking.application.TrackingStore;
import dev.rotadev.tracking.domain.ActivityKind;
import dev.rotadev.tracking.domain.ReviewHabit;
import dev.rotadev.tracking.domain.StudySession;
import dev.rotadev.tracking.domain.TemplateSlot;

@Repository
class JdbcTrackingStore implements TrackingStore {

    private final JdbcClient jdbc;

    JdbcTrackingStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Apaga e regrava: a semana padrão é salva sempre inteira, dentro da transação do caso de uso. */
    @Override
    public void replaceTemplate(UUID userId, List<TemplateSlot> slots) {
        jdbc.sql("DELETE FROM weekly_template_slots WHERE user_id = :userId").param("userId", userId).update();
        for (TemplateSlot s : slots) {
            jdbc.sql("INSERT INTO weekly_template_slots (user_id, weekday, kind, minutes) VALUES (:userId, :weekday, :kind, :minutes)")
                    .param("userId", userId).param("weekday", s.weekday().getValue()).param("kind", s.kind().name())
                    .param("minutes", s.minutes())
                    .update();
        }
    }

    @Override
    public List<TemplateSlot> template(UUID userId) {
        return jdbc.sql("SELECT weekday, kind, minutes FROM weekly_template_slots WHERE user_id = :userId ORDER BY weekday, kind")
                .param("userId", userId)
                .query((rs, n) -> new TemplateSlot(DayOfWeek.of(rs.getInt("weekday")), ActivityKind.valueOf(rs.getString("kind")), rs.getInt("minutes")))
                .list();
    }

    @Override
    public void insertSession(UUID userId, StudySession s, Instant now) {
        jdbc.sql("""
                INSERT INTO study_sessions (id, user_id, day, kind, minutes, topic, learning, created_at)
                VALUES (:id, :userId, :day, :kind, :minutes, :topic, :learning, :now)
                """)
                .param("id", s.id()).param("userId", userId).param("day", Date.valueOf(s.day())).param("kind", s.kind().name())
                .param("minutes", s.minutes()).param("topic", s.topic()).param("learning", s.learning()).param("now", Timestamp.from(now))
                .update();
    }

    @Override
    public List<StudySession> sessions(UUID userId, LocalDate from, LocalDate to) {
        return jdbc.sql("""
                SELECT id, day, kind, minutes, topic, learning FROM study_sessions
                 WHERE user_id = :userId AND day BETWEEN :from AND :to
                 ORDER BY day DESC, created_at DESC
                """)
                .param("userId", userId).param("from", Date.valueOf(from)).param("to", Date.valueOf(to))
                .query((rs, n) -> new StudySession(rs.getObject("id", UUID.class), rs.getDate("day").toLocalDate(),
                        ActivityKind.valueOf(rs.getString("kind")), rs.getInt("minutes"), rs.getString("topic"), rs.getString("learning")))
                .list();
    }

    @Override
    public boolean deleteSession(UUID userId, UUID id) {
        return jdbc.sql("DELETE FROM study_sessions WHERE id = :id AND user_id = :userId")
                .param("id", id).param("userId", userId).update() == 1;
    }

    @Override
    public void upsertHabit(UUID userId, ReviewHabit h, Instant now) {
        jdbc.sql("""
                INSERT INTO review_habits (user_id, day, read_diff, explained, tested, note, updated_at)
                VALUES (:userId, :day, :readDiff, :explained, :tested, :note, :now)
                ON CONFLICT (user_id, day) DO UPDATE SET
                    read_diff = EXCLUDED.read_diff, explained = EXCLUDED.explained, tested = EXCLUDED.tested,
                    note = EXCLUDED.note, updated_at = EXCLUDED.updated_at
                """)
                .param("userId", userId).param("day", Date.valueOf(h.day())).param("readDiff", h.readDiff())
                .param("explained", h.explained()).param("tested", h.tested()).param("note", h.note()).param("now", Timestamp.from(now))
                .update();
    }

    @Override
    public List<ReviewHabit> habits(UUID userId, LocalDate from, LocalDate to) {
        return jdbc.sql("""
                SELECT day, read_diff, explained, tested, note FROM review_habits
                 WHERE user_id = :userId AND day BETWEEN :from AND :to ORDER BY day DESC
                """)
                .param("userId", userId).param("from", Date.valueOf(from)).param("to", Date.valueOf(to))
                .query((rs, n) -> new ReviewHabit(rs.getDate("day").toLocalDate(), rs.getBoolean("read_diff"),
                        rs.getBoolean("explained"), rs.getBoolean("tested"), rs.getString("note")))
                .list();
    }
}

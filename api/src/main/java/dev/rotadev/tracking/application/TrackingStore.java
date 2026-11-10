package dev.rotadev.tracking.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.rotadev.tracking.domain.MilestoneAchievement;
import dev.rotadev.tracking.domain.ReviewHabit;
import dev.rotadev.tracking.domain.StudySession;
import dev.rotadev.tracking.domain.TemplateSlot;
import dev.rotadev.tracking.domain.WeeklyCheckin;

/** Persistência do acompanhamento. Todo método recebe o {@code userId} (ADR 0004). */
public interface TrackingStore {

    void replaceTemplate(UUID userId, List<TemplateSlot> slots);

    List<TemplateSlot> template(UUID userId);

    void insertSession(UUID userId, StudySession session, Instant now);

    /** Sessões com {@code from <= day <= to}, mais recentes primeiro. */
    List<StudySession> sessions(UUID userId, LocalDate from, LocalDate to);

    /** @return {@code false} se a sessão não existe ou é de outro usuário */
    boolean deleteSession(UUID userId, UUID id);

    void upsertHabit(UUID userId, ReviewHabit habit, Instant now);

    List<ReviewHabit> habits(UUID userId, LocalDate from, LocalDate to);

    /** @return {@code false} se já existe check-in para a semana */
    boolean insertCheckin(UUID userId, WeeklyCheckin checkin, Instant now);

    Optional<WeeklyCheckin> checkin(UUID userId, LocalDate weekStart);

    List<WeeklyCheckin> checkins(UUID userId);

    /** @return {@code false} se o portão já estava registrado */
    boolean insertAchievement(UUID userId, MilestoneAchievement achievement, Instant now);

    List<MilestoneAchievement> achievements(UUID userId);
}

package dev.rotadev.tracking.application;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.goals.GoalReader;
import dev.rotadev.identity.UserTimeZones;
import dev.rotadev.plan.PlanReader;
import dev.rotadev.shared.UuidV7;
import dev.rotadev.tracking.domain.ReviewHabit;
import dev.rotadev.tracking.domain.StudySession;
import dev.rotadev.tracking.domain.TemplateSlot;

/**
 * Casos de uso do acompanhamento. Toda data é o dia no fuso do usuário (ADR 0009), e a semana vai de
 * segunda a domingo, porque o check-in é no domingo.
 */
@Service
public class TrackingService {

    /** Até quantos dias para trás dá para registrar uma sessão esquecida. */
    static final int SESSION_BACKFILL_DAYS = 30;
    /** O hábito de revisão é diário: só se corrige a última semana. */
    static final int HABIT_BACKFILL_DAYS = 7;

    private final TrackingStore store;
    private final PlanReader plans;
    private final GoalReader goals;
    private final UserTimeZones timeZones;
    private final Clock clock;

    public TrackingService(TrackingStore store, PlanReader plans, GoalReader goals, UserTimeZones timeZones, Clock clock) {
        this.store = store;
        this.plans = plans;
        this.goals = goals;
        this.timeZones = timeZones;
        this.clock = clock;
    }

    public LocalDate today(UUID userId) {
        return LocalDate.now(clock.withZone(timeZones.of(userId)));
    }

    @Transactional
    public StudySession logSession(UUID userId, StudySession draft) {
        LocalDate day = draft.day() == null ? today(userId) : draft.day();
        requireWithin(userId, day, SESSION_BACKFILL_DAYS);
        var session = new StudySession(UuidV7.generate(clock.instant()), day, draft.kind(), draft.minutes(), draft.topic().trim(),
                draft.learning() == null || draft.learning().isBlank() ? null : draft.learning().trim());
        store.insertSession(userId, session, clock.instant());
        return session;
    }

    @Transactional
    public boolean deleteSession(UUID userId, UUID id) {
        return store.deleteSession(userId, id);
    }

    @Transactional(readOnly = true)
    public List<StudySession> sessions(UUID userId, LocalDate from, LocalDate to) {
        return store.sessions(userId, from, to);
    }

    @Transactional
    public List<TemplateSlot> saveTemplate(UUID userId, List<TemplateSlot> slots) {
        Set<String> seen = new HashSet<>();
        for (TemplateSlot s : slots) {
            if (!seen.add(s.weekday() + ":" + s.kind())) {
                throw new InvalidTrackingException("Cada atividade aparece uma vez por dia na semana padrão.");
            }
        }
        store.replaceTemplate(userId, slots);
        return slots;
    }

    @Transactional(readOnly = true)
    public List<TemplateSlot> template(UUID userId) {
        return store.template(userId);
    }

    @Transactional
    public ReviewHabit saveHabit(UUID userId, ReviewHabit habit) {
        requireWithin(userId, habit.day(), HABIT_BACKFILL_DAYS);
        store.upsertHabit(userId, habit, clock.instant());
        return habit;
    }

    @Transactional(readOnly = true)
    public NowView now(UUID userId) {
        LocalDate today = today(userId);
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<StudySession> week = store.sessions(userId, monday, today);
        List<StudySession> todays = week.stream().filter(s -> s.day().equals(today)).toList();
        List<TemplateSlot> templateToday = store.template(userId).stream().filter(s -> s.weekday() == today.getDayOfWeek()).toList();
        ReviewHabit habit = store.habits(userId, today, today).stream().findFirst().orElse(null);
        Integer budget = goals.find(userId).map(g -> g.hoursPerWeek() * 60).orElse(null);

        return new NowView(today, today.getDayOfWeek(), plans.weekOf(userId, today).orElse(null), templateToday, todays,
                minutes(todays), minutes(week), budget, habit);
    }

    private static int minutes(List<StudySession> sessions) {
        return sessions.stream().mapToInt(StudySession::minutes).sum();
    }

    private void requireWithin(UUID userId, LocalDate day, int backfillDays) {
        LocalDate today = today(userId);
        if (day.isAfter(today)) {
            throw new InvalidTrackingException("Não dá para registrar um dia que ainda não chegou.");
        }
        if (day.isBefore(today.minusDays(backfillDays))) {
            throw new InvalidTrackingException("Só dá para registrar até " + backfillDays + " dias atrás.");
        }
    }
}

package dev.rotadev.tracking.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.plan.PlanReader;
import dev.rotadev.shared.UuidV7;
import dev.rotadev.tracking.CheckinCompleted;
import dev.rotadev.tracking.TrackingReader;
import dev.rotadev.tracking.domain.CheckinWindow;
import dev.rotadev.tracking.domain.MilestoneAchievement;
import dev.rotadev.tracking.domain.WeeklyCheckin;

/** Check-in semanal e portões alcançados. */
@Service
public class CheckinService {

    private final TrackingStore store;
    private final TrackingReader reader;
    private final PlanReader plans;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public CheckinService(TrackingStore store, TrackingReader reader, PlanReader plans, ApplicationEventPublisher events, Clock clock) {
        this.store = store;
        this.reader = reader;
        this.plans = plans;
        this.events = events;
        this.clock = clock;
    }

    /**
     * O que a tela de check-in precisa: qual semana está aberta (se alguma), os números calculados
     * dela e o check-in já feito, se houver.
     */
    public record Status(LocalDate openWeek, int minutesLogged, int reviewDays, WeeklyCheckin done) {
    }

    @Transactional(readOnly = true)
    public Status status(UUID userId) {
        Optional<LocalDate> week = CheckinWindow.weekFor(reader.today(userId));
        if (week.isEmpty()) {
            return new Status(null, 0, 0, null);
        }
        LocalDate monday = week.get();
        LocalDate sunday = monday.plusDays(6);
        return new Status(monday, reader.minutesBetween(userId, monday, sunday), reader.completeReviewDaysBetween(userId, monday, sunday),
                store.checkin(userId, monday).orElse(null));
    }

    /**
     * Grava o check-in da semana aberta com os números calculados agora e publica
     * {@link CheckinCompleted}. O evento é entregue na mesma transação ({@code @EventListener}
     * síncrono): se o ouvinte falhar, o check-in não é gravado pela metade.
     */
    @Transactional
    public WeeklyCheckin submit(UUID userId, int commits, int energy, String blockers, String notes) {
        LocalDate monday = CheckinWindow.weekFor(reader.today(userId))
                .orElseThrow(() -> new InvalidTrackingException("O check-in abre no sábado e fica aberto até terça."));
        LocalDate sunday = monday.plusDays(6);
        var checkin = new WeeklyCheckin(monday, reader.minutesBetween(userId, monday, sunday),
                reader.completeReviewDaysBetween(userId, monday, sunday), commits, energy, blank(blockers), blank(notes));
        if (!store.insertCheckin(userId, checkin, clock.instant())) {
            throw new InvalidTrackingException("O check-in desta semana já foi feito.");
        }
        events.publishEvent(new CheckinCompleted(userId, monday));
        return checkin;
    }

    @Transactional(readOnly = true)
    public List<WeeklyCheckin> history(UUID userId) {
        return store.checkins(userId);
    }

    /** Marca um portão do plano em vigor como alcançado. Só vale portão que existe no plano. */
    @Transactional
    public MilestoneAchievement achieve(UUID userId, String milestone, String evidenceUrl) {
        var plan = plans.current(userId).orElseThrow(() -> new InvalidTrackingException("Sem plano em vigor."));
        if (!plan.milestones().contains(milestone)) {
            throw new InvalidTrackingException("Esse portão não está no plano em vigor.");
        }
        if (!evidenceUrl.startsWith("https://")) {
            throw new InvalidTrackingException("A prova precisa ser um link https (PR, deploy, repositório).");
        }
        var achievement = new MilestoneAchievement(UuidV7.generate(clock.instant()), plan.version(), milestone, evidenceUrl,
                reader.today(userId));
        if (!store.insertAchievement(userId, achievement, clock.instant())) {
            throw new InvalidTrackingException("Esse portão já foi registrado.");
        }
        return achievement;
    }

    @Transactional(readOnly = true)
    public List<MilestoneAchievement> achievements(UUID userId) {
        return store.achievements(userId);
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

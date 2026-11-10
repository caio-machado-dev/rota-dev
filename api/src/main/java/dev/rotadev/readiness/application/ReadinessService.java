package dev.rotadev.readiness.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.diagnosis.DiagnosisReader;
import dev.rotadev.goals.GoalReader;
import dev.rotadev.plan.PlanReader;
import dev.rotadev.readiness.domain.ReadinessInputs;
import dev.rotadev.readiness.domain.ReadinessScore;
import dev.rotadev.tracking.CheckinCompleted;
import dev.rotadev.tracking.TrackingReader;

/**
 * Calcula o índice de prontidão a partir dos outros módulos (só pelas APIs públicas deles) e guarda um
 * ponto a cada check-in.
 */
@Service
public class ReadinessService {

    private final DiagnosisReader diagnoses;
    private final PlanReader plans;
    private final GoalReader goals;
    private final TrackingReader tracking;
    private final ReadinessStore store;
    private final Clock clock;

    public ReadinessService(DiagnosisReader diagnoses, PlanReader plans, GoalReader goals, TrackingReader tracking, ReadinessStore store,
            Clock clock) {
        this.diagnoses = diagnoses;
        this.plans = plans;
        this.goals = goals;
        this.tracking = tracking;
        this.store = store;
        this.clock = clock;
    }

    /** O índice agora, com as 4 semanas que terminam em {@code until} (inclusive). */
    @Transactional(readOnly = true)
    public ReadinessScore compute(UUID userId, LocalDate until) {
        LocalDate from = until.minusDays(27);
        var diagnosis = diagnoses.latestReady(userId);
        int points = diagnosis.map(d -> d.axes().stream().mapToInt(a -> Math.min(a.score(), a.required())).sum()).orElse(0);
        int needed = diagnosis.map(d -> d.axes().stream().mapToInt(DiagnosisReader.AxisScore::required).sum()).orElse(0);
        int gatesTotal = plans.current(userId).map(p -> p.milestones().size()).orElse(0);
        int budget = goals.find(userId).map(g -> g.hoursPerWeek() * 60).orElse(0);

        return ReadinessScore.of(new ReadinessInputs(points, needed, tracking.milestonesAchieved(userId), gatesTotal,
                tracking.minutesBetween(userId, from, until), budget, tracking.completeReviewDaysBetween(userId, from, until)));
    }

    @Transactional(readOnly = true)
    public ReadinessScore current(UUID userId) {
        return compute(userId, tracking.today(userId));
    }

    @Transactional(readOnly = true)
    public List<ReadinessStore.Point> history(UUID userId) {
        return store.history(userId);
    }

    /**
     * Check-in fechado: grava o índice da semana, medido até o domingo dela. Roda na mesma transação do
     * check-in (evento síncrono): os dois entram juntos ou nenhum entra.
     */
    @EventListener
    public void onCheckin(CheckinCompleted event) {
        LocalDate sunday = event.weekStart().plusDays(6);
        store.upsert(event.userId(), event.weekStart(), compute(event.userId(), sunday), clock.instant());
    }
}

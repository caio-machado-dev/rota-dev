package dev.rotadev.adjustment.application;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.adjustment.domain.AdjustmentRules;
import dev.rotadev.adjustment.domain.WeekResult;
import dev.rotadev.goals.GoalReader;
import dev.rotadev.plan.PlanReader;
import dev.rotadev.plan.PlanWriter;
import dev.rotadev.tracking.CheckinCompleted;
import dev.rotadev.tracking.TrackingReader;

/**
 * Reajustes (glossário). O check-in fecha a semana; se as regras virem motivo, fica uma proposta
 * esperando a decisão da pessoa. Aceitar pede uma versão nova do plano com o motivo; recusar só registra.
 */
@Service
public class AdjustmentService {

    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");

    private final AdjustmentStore store;
    private final TrackingReader tracking;
    private final GoalReader goals;
    private final PlanReader plans;
    private final PlanWriter planWriter;
    private final Clock clock;

    public AdjustmentService(AdjustmentStore store, TrackingReader tracking, GoalReader goals, PlanReader plans, PlanWriter planWriter,
            Clock clock) {
        this.store = store;
        this.tracking = tracking;
        this.goals = goals;
        this.plans = plans;
        this.planWriter = planWriter;
        this.clock = clock;
    }

    /** Sem plano em vigor não há o que reajustar. */
    @EventListener
    public void onCheckin(CheckinCompleted event) {
        if (plans.current(event.userId()).isEmpty()) {
            return;
        }
        List<WeekResult> recent = tracking.recentCheckins(event.userId(), 2).stream()
                .map(c -> new WeekResult(c.weekStart(), c.minutesLogged(), c.reviewDays(), c.energy(), c.blockers()))
                .toList();
        int budget = goals.find(event.userId()).map(g -> g.hoursPerWeek() * 60).orElse(0);
        AdjustmentRules.propose(recent, budget)
                .ifPresent(p -> store.insertProposal(event.userId(), event.weekStart(), p, clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<AdjustmentStore.StoredAdjustment> list(UUID userId) {
        return store.list(userId);
    }

    @Transactional
    public UUID accept(UUID userId, UUID id) {
        var adjustment = pending(userId, id);
        String reason = "Reajuste aceito no check-in de " + adjustment.weekStart().format(DAY_MONTH) + ": " + adjustment.proposal().reason();
        UUID planVersion = planWriter.requestVersion(userId, reason, adjustment.proposal().guidance());
        store.decide(userId, id, "ACCEPTED", planVersion, clock.instant());
        return planVersion;
    }

    @Transactional
    public void reject(UUID userId, UUID id) {
        pending(userId, id);
        store.decide(userId, id, "REJECTED", null, clock.instant());
    }

    private AdjustmentStore.StoredAdjustment pending(UUID userId, UUID id) {
        return store.find(userId, id).filter(a -> a.status().equals("PROPOSED")).orElseThrow(AdjustmentNotFoundException::new);
    }
}

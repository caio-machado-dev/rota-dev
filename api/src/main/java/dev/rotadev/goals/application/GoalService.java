package dev.rotadev.goals.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.goals.CareerGoal;
import dev.rotadev.goals.GoalReader;
import dev.rotadev.identity.UserTimeZones;

@Service
public class GoalService implements GoalReader {

    /** Mais que isso deixa de ser um plano e vira um desejo; o plano é revisto a cada trimestre. */
    static final int MAX_YEARS_AHEAD = 5;

    private final GoalStore store;
    private final UserTimeZones timeZones;
    private final Clock clock;

    public GoalService(GoalStore store, UserTimeZones timeZones, Clock clock) {
        this.store = store;
        this.timeZones = timeZones;
        this.clock = clock;
    }

    /**
     * "Hoje" é o dia no fuso do usuário (ADR 0009): às 22h de 31/12 em São Paulo já é 01/01 em UTC,
     * e um prazo "amanhã" seria aceito ou recusado errado se usássemos UTC.
     */
    @Transactional
    public CareerGoal save(CareerGoal goal) {
        LocalDate today = LocalDate.now(clock.withZone(timeZones.of(goal.userId())));
        if (!goal.deadline().isAfter(today)) {
            throw new InvalidGoalException("O prazo precisa ser uma data futura.");
        }
        if (goal.deadline().isAfter(today.plusYears(MAX_YEARS_AHEAD))) {
            throw new InvalidGoalException("Escolha um prazo de até " + MAX_YEARS_AHEAD + " anos.");
        }
        store.upsert(goal, clock.instant());
        return goal;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CareerGoal> find(UUID userId) {
        return store.find(userId);
    }
}

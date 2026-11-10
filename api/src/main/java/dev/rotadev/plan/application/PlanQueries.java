package dev.rotadev.plan.application;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.plan.PlanReader;

@Service
class PlanQueries implements PlanReader {

    private final PlanStore plans;

    PlanQueries(PlanStore plans) {
        this.plans = plans;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CurrentWeek> weekOf(UUID userId, LocalDate today) {
        return plans.current(userId).flatMap(plan -> {
            long days = ChronoUnit.DAYS.between(plan.startsOn(), today);
            if (days < 0 || plan.content() == null || plan.content().quarters().isEmpty()) {
                return Optional.empty();
            }
            int index = (int) (days / 7) + 1;
            return plan.content().quarters().getFirst().weeks().stream()
                    .filter(w -> w.index() == index)
                    .findFirst()
                    .map(w -> new CurrentWeek(plan.version(), w.index(), w.theme(),
                            w.tasks().stream().map(t -> new WeekTask(t.title(), t.kind(), t.minutes())).toList(),
                            w.totalMinutes()));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CurrentPlan> current(UUID userId) {
        return plans.current(userId).map(p -> new CurrentPlan(p.id(), p.version(), p.startsOn(),
                p.content() == null ? 0 : p.content().quarters().stream().mapToInt(q -> q.milestones().size()).sum(),
                p.content() == null ? 0 : p.content().quarters().size()));
    }
}

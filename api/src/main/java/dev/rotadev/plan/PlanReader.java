package dev.rotadev.plan;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** API pública do módulo {@code plan}: o acompanhamento e o check-in leem o plano por aqui. */
public interface PlanReader {

    /**
     * A semana do plano em vigor que contém {@code today}. A semana 1 começa na data de início do plano.
     * Vazio se não há plano pronto ou se a data cai fora das semanas detalhadas.
     */
    Optional<CurrentWeek> weekOf(UUID userId, LocalDate today);

    /** Resumo do plano em vigor, para o check-in e os reajustes. */
    Optional<CurrentPlan> current(UUID userId);

    record CurrentWeek(int planVersion, int index, String theme, List<WeekTask> tasks, int totalMinutes) {
    }

    record WeekTask(String title, String kind, int minutes) {
    }

    record CurrentPlan(UUID id, int version, LocalDate startsOn, int milestoneCount, int quarterCount) {
    }
}

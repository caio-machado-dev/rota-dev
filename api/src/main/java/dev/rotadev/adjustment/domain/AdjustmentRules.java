package dev.rotadev.adjustment.domain;

import java.util.List;
import java.util.Optional;

/**
 * Decide se um check-in pede reajuste. Regra determinística, sem IA: a pessoa precisa entender por que
 * o sistema propôs a mudança, e a mesma semana tem que dar a mesma proposta. A IA só entra depois, se a
 * proposta for aceita, para montar a versão nova do plano.
 *
 * <p>Uma proposta por check-in, na ordem de prioridade: semana difícil, carga baixa, carga alta, revisão.
 */
public final class AdjustmentRules {

    static final double LOW_LOAD = 0.6;
    static final double HIGH_LOAD = 1.2;
    static final int MIN_REVIEW_DAYS = 2;

    private AdjustmentRules() {
    }

    /**
     * @param recent        check-ins mais recentes primeiro (usa até dois)
     * @param weeklyBudget  minutos por semana do objetivo (0 se não houver objetivo)
     */
    public static Optional<Proposal> propose(List<WeekResult> recent, int weeklyBudget) {
        if (recent.isEmpty()) {
            return Optional.empty();
        }
        WeekResult last = recent.getFirst();

        if (last.energy() <= 2 && last.blockers() != null && !last.blockers().isBlank()) {
            return Optional.of(new Proposal(AdjustmentKind.LIGHTER_WEEK,
                    "Semana pesada: energia " + last.energy() + "/5 e um bloqueio (" + last.blockers() + ").",
                    "Na próxima semana, reduzir a carga em 40% e priorizar tarefas curtas; manter a revisão diária. "
                            + "Bloqueio relatado: " + last.blockers()));
        }

        if (weeklyBudget > 0 && recent.size() >= 2) {
            WeekResult previous = recent.get(1);
            int average = (last.minutesLogged() + previous.minutesLogged()) / 2;
            if (last.minutesLogged() < weeklyBudget * LOW_LOAD && previous.minutesLogged() < weeklyBudget * LOW_LOAD) {
                int hours = Math.max(1, Math.round(average / 60f));
                return Optional.of(new Proposal(AdjustmentKind.REDUCE_LOAD,
                        "Duas semanas abaixo de 60% das horas planejadas (média de " + hours + " h).",
                        "Replanejar com carga de " + hours + " h por semana, a média real das duas últimas semanas, "
                                + "mantendo os portões e alongando os prazos internos se preciso."));
            }
            if (last.minutesLogged() > weeklyBudget * HIGH_LOAD && previous.minutesLogged() > weeklyBudget * HIGH_LOAD) {
                int hours = Math.round(average / 60f);
                return Optional.of(new Proposal(AdjustmentKind.INCREASE_LOAD,
                        "Duas semanas acima de 120% das horas planejadas (média de " + hours + " h).",
                        "Há folga: a pessoa fez em média " + hours + " h por semana. Antecipar o próximo portão, "
                                + "sem passar dessa média."));
            }
        }

        if (last.reviewDays() < MIN_REVIEW_DAYS) {
            return Optional.of(new Proposal(AdjustmentKind.MORE_REVIEW,
                    "Só " + last.reviewDays() + " dia(s) com o protocolo de revisão completo na semana.",
                    "Toda semana com pelo menos 3 tarefas REVIEW de 20 a 30 minutos, ligadas ao projeto-âncora."));
        }
        return Optional.empty();
    }
}

package dev.rotadev.plan.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Confere o plano que a IA propôs. Como no diagnóstico, a IA sugere e esta regra decide.
 *
 * <ul>
 *   <li><b>Horizonte:</b> um trimestre a cada 3 meses até o prazo (de 1 a {@value #MAX_QUARTERS}); o excedente é cortado.</li>
 *   <li><b>Carga:</b> nenhuma semana passa das horas que o usuário declarou. As tarefas entram na ordem
 *       proposta até o limite; o resto fica de fora (a IA tende a ser otimista).</li>
 *   <li><b>Portões:</b> só valem os que apontam para um eixo do diagnóstico e têm critério verificável.</li>
 *   <li><b>Certificação:</b> só entra se o usuário marcou que quer.</li>
 *   <li><b>Projeto-âncora:</b> só cita evidências que existem no perfil.</li>
 * </ul>
 */
public final class PlanRules {

    public static final int MAX_QUARTERS = 8;
    private static final Set<String> TASK_KINDS = Set.of("STUDY", "BUILD", "REVIEW", "CERT");

    private PlanRules() {
    }

    public static int quartersUntil(LocalDate today, LocalDate deadline) {
        long months = Math.max(1, ChronoUnit.MONTHS.between(today, deadline));
        return (int) Math.clamp((months + 2) / 3, 1, MAX_QUARTERS);
    }

    public static PlanContent reconcile(AiPlan proposed, int quarters, int hoursPerWeek, Set<String> axisKeys,
            Set<UUID> knownEvidence, boolean wantsCertifications) {
        int weeklyBudget = hoursPerWeek * 60;

        List<PlanContent.Quarter> kept = new ArrayList<>();
        for (PlanContent.Quarter q : proposed.quarters()) {
            if (kept.size() == quarters) {
                break;
            }
            List<PlanContent.Milestone> milestones = q.milestones().stream()
                    .filter(m -> axisKeys.contains(m.axisKey()) && !m.criteria().isEmpty())
                    .toList();
            List<PlanContent.Week> weeks = kept.isEmpty()
                    ? q.weeks().stream().map(w -> fit(w, weeklyBudget, wantsCertifications)).toList()
                    : List.of();
            kept.add(new PlanContent.Quarter(kept.size() + 1, q.title(), q.focus(), milestones, weeks));
        }

        List<PlanContent.Resource> resources = proposed.resources().stream()
                .filter(r -> wantsCertifications || !r.kind().equals("CERT"))
                .toList();

        AiPlan.AiAnchor ai = proposed.anchorProject();
        List<UUID> evidence = ai.evidenceIds().stream()
                .map(PlanRules::parseUuid)
                .filter(id -> id != null && knownEvidence.contains(id))
                .distinct()
                .toList();
        var anchor = new PlanContent.AnchorProject(ai.title(), ai.description(), ai.deliverables(), evidence);

        return new PlanContent(proposed.summary(), anchor, kept, resources);
    }

    /** Mantém as tarefas na ordem até o orçamento da semana; descarta tipos desconhecidos e minutos inválidos. */
    private static PlanContent.Week fit(PlanContent.Week week, int budgetMinutes, boolean wantsCertifications) {
        List<PlanContent.Task> tasks = new ArrayList<>();
        int used = 0;
        for (PlanContent.Task t : week.tasks()) {
            if (!TASK_KINDS.contains(t.kind()) || t.minutes() <= 0 || (t.kind().equals("CERT") && !wantsCertifications)) {
                continue;
            }
            if (used + t.minutes() > budgetMinutes) {
                break;
            }
            tasks.add(t);
            used += t.minutes();
        }
        return new PlanContent.Week(week.index(), week.theme(), tasks);
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}

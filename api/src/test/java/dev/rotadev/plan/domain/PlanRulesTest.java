package dev.rotadev.plan.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.rotadev.plan.domain.PlanContent.Milestone;
import dev.rotadev.plan.domain.PlanContent.Quarter;
import dev.rotadev.plan.domain.PlanContent.Resource;
import dev.rotadev.plan.domain.PlanContent.Task;
import dev.rotadev.plan.domain.PlanContent.Week;

class PlanRulesTest {

    private static final UUID REPO = UUID.randomUUID();

    @Test
    void quartersFollowTheDeadline() {
        LocalDate today = LocalDate.of(2026, 11, 10);

        assertThat(PlanRules.quartersUntil(today, LocalDate.of(2027, 2, 10))).isEqualTo(1);
        assertThat(PlanRules.quartersUntil(today, LocalDate.of(2027, 8, 10))).isEqualTo(3);
        assertThat(PlanRules.quartersUntil(today, LocalDate.of(2031, 1, 1))).isEqualTo(PlanRules.MAX_QUARTERS);
    }

    @Test
    void weekNeverExceedsDeclaredHours() {
        var week = new Week(1, "Base", List.of(
                new Task("Ler capítulo de testes", "STUDY", 120),
                new Task("Escrever testes do módulo", "BUILD", 180),
                new Task("Revisar diff do agente", "REVIEW", 60)));

        var plan = PlanRules.reconcile(plan(List.of(quarter(List.of(week)))), 1, 5, Set.of("testing"), Set.of(REPO), false);

        Week fitted = plan.quarters().getFirst().weeks().getFirst();
        assertThat(fitted.totalMinutes()).isLessThanOrEqualTo(5 * 60);
        assertThat(fitted.tasks()).extracting(Task::title).containsExactly("Ler capítulo de testes", "Escrever testes do módulo");
    }

    @Test
    void dropsExtraQuartersInvalidMilestonesCertsAndFakeEvidence() {
        var q1 = new Quarter(1, "T1", "Testes", List.of(
                new Milestone("Testes de integração no CI", "testing", List.of("PR mergeado com Testcontainers")),
                new Milestone("Inventado", "blockchain", List.of("algo")),
                new Milestone("Sem critério", "testing", List.of())), List.of(
                new Week(1, "Base", List.of(new Task("Prova AWS", "CERT", 60), new Task("Revisar diff", "REVIEW", 30)))));
        var q2 = new Quarter(2, "T2", "APIs", List.of(), List.of(new Week(1, "Não deveria ter semanas", List.of())));
        var proposed = new AiPlan("Resumo",
                new AiPlan.AiAnchor("Rota Dev", "Continuar o projeto", List.of("API publicada"), List.of(REPO.toString(), UUID.randomUUID().toString(), "x")),
                List.of(q1, q2, q1),
                List.of(new Resource("AWS Developer", "CERT", "delivery", "Pedido de vaga"), new Resource("Livro de testes", "BOOK", "testing", "Lacuna 1")));

        var plan = PlanRules.reconcile(proposed, 2, 10, Set.of("testing", "delivery"), Set.of(REPO), false);

        assertThat(plan.quarters()).hasSize(2);
        assertThat(plan.quarters().getFirst().milestones()).extracting(Milestone::title).containsExactly("Testes de integração no CI");
        assertThat(plan.quarters().getFirst().weeks().getFirst().tasks()).extracting(Task::kind).containsExactly("REVIEW");
        assertThat(plan.quarters().get(1).weeks()).isEmpty();
        assertThat(plan.resources()).extracting(Resource::kind).containsExactly("BOOK");
        assertThat(plan.anchorProject().evidenceIds()).containsExactly(REPO);
    }

    private static Quarter quarter(List<Week> weeks) {
        return new Quarter(1, "T1", "Testes", List.of(), weeks);
    }

    private static AiPlan plan(List<Quarter> quarters) {
        return new AiPlan("Resumo", new AiPlan.AiAnchor("Projeto", "", List.of(), List.of()), quarters, List.of());
    }
}

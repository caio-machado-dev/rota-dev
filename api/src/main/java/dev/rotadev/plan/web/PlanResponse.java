package dev.rotadev.plan.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import dev.rotadev.plan.application.PlanStore.StoredPlan;
import dev.rotadev.plan.domain.PlanContent;
import jakarta.validation.constraints.NotNull;

/**
 * Uma versão do plano. A árvore é espelhada em records da camada web (ADR 0003): parece repetição,
 * mas é ela que deixa o domínio mudar sem quebrar o contrato do front.
 *
 * <p>Os nomes têm o prefixo {@code Plan} porque o springdoc usa o nome simples da classe no OpenAPI:
 * um {@code Task} aqui e outro no acompanhamento sobrescreveriam um ao outro no contrato.
 */
public record PlanResponse(
        @NotNull UUID id, @NotNull int version, @NotNull String status, @NotNull String reason, @NotNull LocalDate startsOn,
        @NotNull Instant createdAt, PlanContentView content, String error) {

    public record PlanContentView(@NotNull String summary, @NotNull PlanAnchor anchorProject, @NotNull List<PlanQuarter> quarters,
            @NotNull List<PlanResource> resources) {
    }

    public record PlanAnchor(@NotNull String title, @NotNull String description, @NotNull List<String> deliverables,
            @NotNull List<UUID> evidenceIds) {
    }

    public record PlanQuarter(@NotNull int index, @NotNull String title, @NotNull String focus, @NotNull List<PlanMilestone> milestones,
            @NotNull List<PlanWeek> weeks) {
    }

    public record PlanMilestone(@NotNull String title, @NotNull String axisKey, @NotNull List<String> criteria) {
    }

    public record PlanWeek(@NotNull int index, @NotNull String theme, @NotNull List<PlanTask> tasks, @NotNull int totalMinutes) {
    }

    public record PlanTask(@NotNull String title, @NotNull String kind, @NotNull int minutes) {
    }

    public record PlanResource(@NotNull String title, @NotNull String kind, @NotNull String axisKey, @NotNull String reason) {
    }

    static PlanResponse from(StoredPlan p) {
        return new PlanResponse(p.id(), p.version(), p.status(), p.reason(), p.startsOn(), p.createdAt(),
                p.content() == null ? null : content(p.content()), p.error());
    }

    private static PlanContentView content(PlanContent c) {
        var a = c.anchorProject();
        return new PlanContentView(
                c.summary(),
                new PlanAnchor(a.title(), a.description(), a.deliverables(), a.evidenceIds()),
                c.quarters().stream().map(q -> new PlanQuarter(q.index(), q.title(), q.focus(),
                        q.milestones().stream().map(m -> new PlanMilestone(m.title(), m.axisKey(), m.criteria())).toList(),
                        q.weeks().stream().map(w -> new PlanWeek(w.index(), w.theme(),
                                w.tasks().stream().map(t -> new PlanTask(t.title(), t.kind(), t.minutes())).toList(),
                                w.totalMinutes())).toList())).toList(),
                c.resources().stream().map(r -> new PlanResource(r.title(), r.kind(), r.axisKey(), r.reason())).toList());
    }
}

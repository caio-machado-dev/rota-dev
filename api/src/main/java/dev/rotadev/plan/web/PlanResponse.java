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
 */
public record PlanResponse(
        @NotNull UUID id, @NotNull int version, @NotNull String status, @NotNull String reason, @NotNull LocalDate startsOn,
        @NotNull Instant createdAt, Content content, String error) {

    public record Content(@NotNull String summary, @NotNull Anchor anchorProject, @NotNull List<Quarter> quarters,
            @NotNull List<Resource> resources) {
    }

    public record Anchor(@NotNull String title, @NotNull String description, @NotNull List<String> deliverables,
            @NotNull List<UUID> evidenceIds) {
    }

    public record Quarter(@NotNull int index, @NotNull String title, @NotNull String focus, @NotNull List<Milestone> milestones,
            @NotNull List<Week> weeks) {
    }

    public record Milestone(@NotNull String title, @NotNull String axisKey, @NotNull List<String> criteria) {
    }

    public record Week(@NotNull int index, @NotNull String theme, @NotNull List<Task> tasks, @NotNull int totalMinutes) {
    }

    public record Task(@NotNull String title, @NotNull String kind, @NotNull int minutes) {
    }

    public record Resource(@NotNull String title, @NotNull String kind, @NotNull String axisKey, @NotNull String reason) {
    }

    static PlanResponse from(StoredPlan p) {
        return new PlanResponse(p.id(), p.version(), p.status(), p.reason(), p.startsOn(), p.createdAt(),
                p.content() == null ? null : content(p.content()), p.error());
    }

    private static Content content(PlanContent c) {
        var a = c.anchorProject();
        return new Content(
                c.summary(),
                new Anchor(a.title(), a.description(), a.deliverables(), a.evidenceIds()),
                c.quarters().stream().map(q -> new Quarter(q.index(), q.title(), q.focus(),
                        q.milestones().stream().map(m -> new Milestone(m.title(), m.axisKey(), m.criteria())).toList(),
                        q.weeks().stream().map(w -> new Week(w.index(), w.theme(),
                                w.tasks().stream().map(t -> new Task(t.title(), t.kind(), t.minutes())).toList(),
                                w.totalMinutes())).toList())).toList(),
                c.resources().stream().map(r -> new Resource(r.title(), r.kind(), r.axisKey(), r.reason())).toList());
    }
}

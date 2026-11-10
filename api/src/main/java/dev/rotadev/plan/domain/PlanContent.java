package dev.rotadev.plan.domain;

import java.util.List;
import java.util.UUID;

/**
 * Conteúdo de uma versão do plano (glossário), já conferido pelas {@link PlanRules}.
 *
 * <ul>
 *   <li>{@link AnchorProject}: o projeto prático central, derivado do histórico real do usuário;</li>
 *   <li>{@link Quarter}: um trimestre com foco e portões; só o primeiro traz as semanas detalhadas;</li>
 *   <li>{@link Milestone}: portão, marco verificável, com critérios objetivos;</li>
 *   <li>{@link Resource}: curso, livro ou certificação, só quando agrega.</li>
 * </ul>
 */
public record PlanContent(String summary, AnchorProject anchorProject, List<Quarter> quarters, List<Resource> resources) {

    public record AnchorProject(String title, String description, List<String> deliverables, List<UUID> evidenceIds) {
    }

    public record Quarter(int index, String title, String focus, List<Milestone> milestones, List<Week> weeks) {
    }

    /** @param axisKey eixo do diagnóstico que este portão prova */
    public record Milestone(String title, String axisKey, List<String> criteria) {
    }

    public record Week(int index, String theme, List<Task> tasks) {

        public int totalMinutes() {
            return tasks.stream().mapToInt(Task::minutes).sum();
        }
    }

    /** @param kind STUDY, BUILD, REVIEW ou CERT */
    public record Task(String title, String kind, int minutes) {
    }

    /** @param kind COURSE, BOOK ou CERT */
    public record Resource(String title, String kind, String axisKey, String reason) {
    }
}

package dev.rotadev.plan.domain;

import java.util.List;

/**
 * O plano como a IA devolve ({@code prompts/plan/v1.schema.json}). Igual ao {@link PlanContent}, exceto
 * que os ids de evidência chegam como texto: um id malformado não pode derrubar a leitura do JSON.
 */
public record AiPlan(String summary, AiAnchor anchorProject, List<PlanContent.Quarter> quarters, List<PlanContent.Resource> resources) {

    public record AiAnchor(String title, String description, List<String> deliverables, List<String> evidenceIds) {
    }
}

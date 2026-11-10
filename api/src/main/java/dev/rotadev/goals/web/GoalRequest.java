package dev.rotadev.goals.web;

import java.time.LocalDate;

import dev.rotadev.goals.Level;
import dev.rotadev.goals.Track;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo do {@code PUT /api/goal}. As anotações são o Bean Validation: com {@code @Valid} no
 * controller, o Spring confere tudo antes de chamar o método e responde 400 se algo falhar,
 * como um modelo Pydantic no FastAPI. O {@code userId} não está aqui: vem da sessão (ADR 0004).
 */
public record GoalRequest(
        @NotBlank @Size(max = 120) String targetRole,
        @NotNull Level targetLevel,
        @NotNull Track track,
        @NotNull LocalDate deadline,
        @Min(1) @Max(60) int hoursPerWeek,
        @Size(max = 200) String primaryStack,
        boolean wantsCertifications,
        @Size(max = 2000) String notes) {
}

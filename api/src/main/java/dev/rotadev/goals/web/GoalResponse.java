package dev.rotadev.goals.web;

import java.time.LocalDate;

import dev.rotadev.goals.CareerGoal;
import jakarta.validation.constraints.NotNull;

public record GoalResponse(
        @NotNull String targetRole,
        @NotNull String targetLevel,
        @NotNull String track,
        @NotNull LocalDate deadline,
        @NotNull int hoursPerWeek,
        String primaryStack,
        @NotNull boolean wantsCertifications,
        String notes) {

    static GoalResponse from(CareerGoal g) {
        return new GoalResponse(g.targetRole(), g.targetLevel().name(), g.track().name(), g.deadline(), g.hoursPerWeek(),
                g.primaryStack(), g.wantsCertifications(), g.notes());
    }
}

package dev.rotadev.readiness.domain;

/**
 * Números brutos que entram no índice.
 *
 * @param skillPoints        soma, por eixo do diagnóstico, de min(nota atual, nota exigida)
 * @param skillPointsNeeded  soma das notas exigidas (0 sem diagnóstico)
 * @param milestonesAchieved portões que a pessoa provou
 * @param milestonesTotal    portões do plano em vigor (0 sem plano)
 * @param studyMinutes       minutos registrados nas últimas 4 semanas
 * @param weeklyBudget       minutos por semana do objetivo (0 sem objetivo)
 * @param reviewDays         dias com o protocolo anti vibe coding completo nas últimas 4 semanas
 */
public record ReadinessInputs(int skillPoints, int skillPointsNeeded, int milestonesAchieved, int milestonesTotal,
        int studyMinutes, int weeklyBudget, int reviewDays) {
}

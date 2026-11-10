package dev.rotadev.readiness.domain;

/**
 * Índice de prontidão (glossário): nota de 0 a 100 composta por quatro partes, cada uma de 0 a 100.
 *
 * <p>Pesos: habilidades 40, portões 25, estudo 20, hábito 15. Habilidades pesam mais porque são o
 * que o cargo-alvo cobra; portões vêm em seguida porque são a prova pública; estudo e hábito medem
 * constância, e sozinhos não fazem ninguém estar pronto.
 */
public record ReadinessScore(int score, int skills, int gates, int study, int habit) {

    static final int SKILLS_WEIGHT = 40;
    static final int GATES_WEIGHT = 25;
    static final int STUDY_WEIGHT = 20;
    static final int HABIT_WEIGHT = 15;
    /** Meta do hábito: 5 dias por semana, em 4 semanas. */
    static final int HABIT_TARGET_DAYS = 20;
    static final int WEEKS = 4;

    public static ReadinessScore of(ReadinessInputs in) {
        double skills = ratio(in.skillPoints(), in.skillPointsNeeded());
        double gates = ratio(in.milestonesAchieved(), in.milestonesTotal());
        double study = ratio(in.studyMinutes(), in.weeklyBudget() * WEEKS);
        double habit = ratio(in.reviewDays(), HABIT_TARGET_DAYS);
        double total = SKILLS_WEIGHT * skills + GATES_WEIGHT * gates + STUDY_WEIGHT * study + HABIT_WEIGHT * habit;
        return new ReadinessScore((int) Math.round(total), pct(skills), pct(gates), pct(study), pct(habit));
    }

    /** Parte sobre todo, limitada a 1. Sem "todo" (sem plano, sem objetivo), a parte vale 0: não há o que medir. */
    private static double ratio(int part, int whole) {
        return whole <= 0 ? 0 : Math.min(1.0, (double) part / whole);
    }

    private static int pct(double ratio) {
        return (int) Math.round(ratio * 100);
    }
}

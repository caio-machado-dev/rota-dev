package dev.rotadev.tracking.application;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import dev.rotadev.plan.PlanReader;
import dev.rotadev.tracking.domain.ReviewHabit;
import dev.rotadev.tracking.domain.StudySession;
import dev.rotadev.tracking.domain.TemplateSlot;

/**
 * Tudo o que o painel "Agora" mostra, calculado para o "hoje" no fuso do usuário.
 *
 * @param planWeek             semana do plano em vigor, ou {@code null} sem plano
 * @param weeklyBudgetMinutes  horas por semana do objetivo × 60, ou {@code null} sem objetivo
 * @param habitToday           registro de hoje do protocolo anti vibe coding, ou {@code null}
 */
public record NowView(
        LocalDate today,
        DayOfWeek weekday,
        PlanReader.CurrentWeek planWeek,
        List<TemplateSlot> templateToday,
        List<StudySession> sessionsToday,
        int minutesToday,
        int minutesThisWeek,
        Integer weeklyBudgetMinutes,
        ReviewHabit habitToday) {
}

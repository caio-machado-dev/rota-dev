package dev.rotadev.plan.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.diagnosis.DiagnosisReader;
import dev.rotadev.goals.GoalReader;
import dev.rotadev.identity.UserTimeZones;
import dev.rotadev.jobs.JobQueue;

/**
 * Caso de uso: gerar uma versão nova do plano a partir do objetivo e do diagnóstico pronto mais recente.
 * Cada pedido vira uma versão; a anterior continua no histórico.
 */
@Service
public class RequestPlan {

    private final GoalReader goals;
    private final DiagnosisReader diagnoses;
    private final PlanStore plans;
    private final JobQueue jobs;
    private final UserTimeZones timeZones;
    private final Clock clock;

    public RequestPlan(GoalReader goals, DiagnosisReader diagnoses, PlanStore plans, JobQueue jobs, UserTimeZones timeZones, Clock clock) {
        this.goals = goals;
        this.diagnoses = diagnoses;
        this.plans = plans;
        this.jobs = jobs;
        this.timeZones = timeZones;
        this.clock = clock;
    }

    public record Created(UUID id, int version) {
    }

    @Transactional
    public Created request(UUID userId, String reason) {
        if (goals.find(userId).isEmpty()) {
            throw new PlanNotPossibleException("Defina o seu objetivo de carreira antes do plano.");
        }
        var diagnosis = diagnoses.latestReady(userId)
                .orElseThrow(() -> new PlanNotPossibleException("Gere o diagnóstico antes do plano."));
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock.withZone(timeZones.of(userId)));
        int version = plans.nextVersion(userId);
        String why = reason != null ? reason : version == 1 ? "Primeira versão" : "Plano refeito a pedido";
        UUID id = plans.insertPending(userId, version, why, diagnosis.id(), today, now);
        jobs.enqueue(userId, GeneratePlan.TYPE, "{\"planVersionId\": \"" + id + "\"}", GeneratePlan.TYPE + ":" + id);
        return new Created(id, version);
    }
}

package dev.rotadev.diagnosis.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.goals.CareerGoal;
import dev.rotadev.ai.AiQuota;
import dev.rotadev.goals.GoalReader;
import dev.rotadev.jobs.JobQueue;
import dev.rotadev.profile.ProfileReader;
import tools.jackson.databind.json.JsonMapper;

/**
 * Caso de uso: o usuário pediu um diagnóstico. Confere se há objetivo e perfil, cria o registro
 * PENDING e enfileira a geração (a chamada à IA leva dezenas de segundos e não cabe na requisição).
 */
@Service
public class RequestDiagnosis {

    private final GoalReader goals;
    private final ProfileReader profile;
    private final DiagnosisStore store;
    private final JobQueue jobs;
    private final JsonMapper json;
    private final AiQuota quota;
    private final Clock clock;

    public RequestDiagnosis(GoalReader goals, ProfileReader profile, DiagnosisStore store, JobQueue jobs, JsonMapper json, AiQuota quota,
            Clock clock) {
        this.goals = goals;
        this.profile = profile;
        this.store = store;
        this.jobs = jobs;
        this.json = json;
        this.quota = quota;
        this.clock = clock;
    }

    @Transactional
    public UUID request(UUID userId) {
        CareerGoal goal = goals.find(userId)
                .orElseThrow(() -> new DiagnosisNotPossibleException("Defina o seu objetivo de carreira antes do diagnóstico."));
        if (profile.latestPerSource(userId).isEmpty()) {
            throw new DiagnosisNotPossibleException("Conecte o GitHub ou envie o currículo antes do diagnóstico.");
        }
        // Antes de enfileirar: barrar depois da chamada à IA já seria custo gasto.
        quota.requireAvailable(userId);
        Instant now = clock.instant();
        UUID id = store.insertPending(userId, json.writeValueAsString(goal), now);
        jobs.enqueue(userId, GenerateDiagnosis.TYPE, "{\"diagnosisId\": \"" + id + "\"}", GenerateDiagnosis.TYPE + ":" + id);
        return id;
    }
}

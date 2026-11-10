package dev.rotadev.plan.application;

import java.time.Clock;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import dev.rotadev.ai.AiCallFailedException;
import dev.rotadev.ai.AiRequest;
import dev.rotadev.ai.AiResult;
import dev.rotadev.ai.CareerAiClient;
import dev.rotadev.diagnosis.DiagnosisReader;
import dev.rotadev.goals.CareerGoal;
import dev.rotadev.goals.GoalReader;
import dev.rotadev.jobs.Job;
import dev.rotadev.jobs.JobHandler;
import dev.rotadev.plan.domain.AiPlan;
import dev.rotadev.plan.domain.PlanContent;
import dev.rotadev.plan.domain.PlanRules;
import dev.rotadev.profile.Evidence;
import dev.rotadev.profile.ProfileReader;
import tools.jackson.databind.json.JsonMapper;

/**
 * Job {@value #TYPE}: monta a entrada (objetivo, diagnóstico, evidências, horizonte), chama a IA com o
 * prompt {@code plan} v{@value #PROMPT_VERSION} e passa a resposta pelas {@link PlanRules}.
 */
@Component
public class GeneratePlan implements JobHandler {

    public static final String TYPE = "plan.generate";
    /** v2: aceita a orientação de um reajuste ({@code adjustment}). */
    static final int PROMPT_VERSION = 2;

    private final PlanStore plans;
    private final GoalReader goals;
    private final DiagnosisReader diagnoses;
    private final ProfileReader profile;
    private final CareerAiClient ai;
    private final JsonMapper json;
    private final Clock clock;

    public GeneratePlan(PlanStore plans, GoalReader goals, DiagnosisReader diagnoses, ProfileReader profile, CareerAiClient ai,
            JsonMapper json, Clock clock) {
        this.plans = plans;
        this.goals = goals;
        this.diagnoses = diagnoses;
        this.profile = profile;
        this.ai = ai;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void handle(Job job) {
        UUID id = UUID.fromString(json.readTree(job.payload()).get("planVersionId").asString());
        var stored = plans.find(job.userId(), id).orElseThrow(() -> new IllegalStateException("plano " + id + " não existe"));
        if (!stored.status().equals("PENDING")) {
            return;
        }
        CareerGoal goal = goals.find(job.userId()).orElseThrow(() -> new IllegalStateException("objetivo sumiu"));
        var diagnosis = diagnoses.latestReady(job.userId()).orElseThrow(() -> new IllegalStateException("diagnóstico sumiu"));
        List<Evidence> evidences = profile.latestPerSource(job.userId()).stream().flatMap(s -> s.evidences().stream()).toList();
        int quarters = PlanRules.quartersUntil(stored.startsOn(), goal.deadline());

        AiResult result;
        try {
            result = ai.run(new AiRequest(job.userId(), "plan", PROMPT_VERSION, input(goal, stored.startsOn().toString(), quarters, diagnosis, evidences, stored.guidance())));
        } catch (AiCallFailedException e) {
            if (e.retryable()) {
                throw e;
            }
            plans.markFailed(job.userId(), id, e.getMessage(), clock.instant());
            return;
        }

        Set<String> axisKeys = diagnosis.axes().stream().map(DiagnosisReader.AxisScore::key).collect(Collectors.toSet());
        Set<UUID> known = new HashSet<>();
        evidences.forEach(e -> known.add(e.id()));
        PlanContent content = PlanRules.reconcile(json.readValue(result.json(), AiPlan.class), quarters, goal.hoursPerWeek(),
                axisKeys, known, goal.wantsCertifications());
        plans.markReady(job.userId(), id, content, result.callId(), clock.instant());
    }

    private String input(CareerGoal goal, String today, int quarters, DiagnosisReader.ReadyDiagnosis diagnosis, List<Evidence> evidences,
            String guidance) {
        Map<String, Object> in = new LinkedHashMap<>();
        in.put("goal", Map.of("targetRole", goal.targetRole(), "targetLevel", goal.targetLevel(), "track", goal.track(),
                "deadline", goal.deadline().toString(), "hoursPerWeek", goal.hoursPerWeek()));
        in.put("today", today);
        in.put("quarters", quarters);
        in.put("wantsCertifications", goal.wantsCertifications());
        if (guidance != null) {
            in.put("adjustment", guidance);
        }
        in.put("diagnosis", Map.of("estimatedLevel", diagnosis.estimatedLevel(), "axes", diagnosis.axes(), "gaps", diagnosis.gaps()));
        in.put("evidences", evidences.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.id().toString());
            m.put("kind", e.kind());
            m.put("skill", e.skill());
            m.put("title", e.title());
            m.put("detail", e.detail());
            return m;
        }).toList());
        return json.writeValueAsString(in);
    }
}

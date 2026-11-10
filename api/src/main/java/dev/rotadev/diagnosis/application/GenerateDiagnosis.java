package dev.rotadev.diagnosis.application;

import java.time.Clock;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import dev.rotadev.ai.AiCallFailedException;
import dev.rotadev.ai.AiRequest;
import dev.rotadev.ai.AiResult;
import dev.rotadev.ai.CareerAiClient;
import dev.rotadev.diagnosis.domain.AiDiagnosis;
import dev.rotadev.diagnosis.domain.Axis;
import dev.rotadev.diagnosis.domain.CompetencyMatrix;
import dev.rotadev.diagnosis.domain.DiagnosisResult;
import dev.rotadev.diagnosis.domain.DiagnosisRules;
import dev.rotadev.goals.CareerGoal;
import dev.rotadev.jobs.Job;
import dev.rotadev.jobs.JobHandler;
import dev.rotadev.profile.Evidence;
import dev.rotadev.profile.ProfileReader;
import dev.rotadev.profile.Snapshot;
import tools.jackson.databind.json.JsonMapper;

/**
 * Job {@value #TYPE}: monta a entrada (objetivo, eixos da matriz, perfil com as evidências), chama a
 * IA com o prompt {@code diagnosis} v{@value #PROMPT_VERSION} e passa a resposta pelas
 * {@link DiagnosisRules} antes de gravar.
 *
 * <p>Idempotente: se o diagnóstico já não estiver PENDING (o job rodou de novo depois de concluir),
 * não faz nada.
 */
@Component
public class GenerateDiagnosis implements JobHandler {

    public static final String TYPE = "diagnosis.generate";
    static final int PROMPT_VERSION = 1;

    private final DiagnosisStore store;
    private final ProfileReader profile;
    private final CareerAiClient ai;
    private final JsonMapper json;
    private final Clock clock;

    public GenerateDiagnosis(DiagnosisStore store, ProfileReader profile, CareerAiClient ai, JsonMapper json, Clock clock) {
        this.store = store;
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
        UUID id = UUID.fromString(json.readTree(job.payload()).get("diagnosisId").asString());
        var stored = store.find(job.userId(), id).orElseThrow(() -> new IllegalStateException("diagnóstico " + id + " não existe"));
        if (!stored.status().equals("PENDING")) {
            return;
        }
        CareerGoal goal = json.readValue(stored.goalJson(), CareerGoal.class);
        List<Axis> axes = CompetencyMatrix.axesFor(goal.track(), goal.targetLevel());
        List<Snapshot> snapshots = profile.latestPerSource(job.userId());

        Set<UUID> known = new HashSet<>();
        snapshots.forEach(s -> s.evidences().forEach(e -> known.add(e.id())));

        AiResult result;
        try {
            result = ai.run(new AiRequest(job.userId(), "diagnosis", PROMPT_VERSION, input(goal, axes, snapshots)));
        } catch (AiCallFailedException e) {
            if (e.retryable()) {
                throw e; // o worker reagenda com backoff
            }
            store.markFailed(job.userId(), id, e.getMessage(), clock.instant());
            return;
        }
        AiDiagnosis answer = json.readValue(result.json(), AiDiagnosis.class);
        DiagnosisResult diagnosis = DiagnosisRules.reconcile(axes, known, answer);
        store.markReady(job.userId(), id, diagnosis, result.callId(), clock.instant());
    }

    /** A entrada leva só o necessário: nada de token, e-mail ou texto bruto dos arquivos. */
    private String input(CareerGoal goal, List<Axis> axes, List<Snapshot> snapshots) {
        Map<String, Object> in = new LinkedHashMap<>();
        in.put("goal", Map.of("targetRole", goal.targetRole(), "targetLevel", goal.targetLevel(), "track", goal.track(),
                "deadline", goal.deadline().toString(), "hoursPerWeek", goal.hoursPerWeek()));
        in.put("axes", axes);
        in.put("profile", snapshots.stream().map(s -> Map.of(
                "source", s.sourceType(),
                "skills", s.skills(),
                "evidences", s.evidences().stream().map(GenerateDiagnosis::evidence).toList())).toList());
        return json.writeValueAsString(in);
    }

    private static Map<String, Object> evidence(Evidence e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.id().toString());
        m.put("kind", e.kind());
        m.put("skill", e.skill());
        m.put("title", e.title());
        m.put("detail", e.detail());
        m.put("confidence", e.confidence());
        return m;
    }
}

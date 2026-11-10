package dev.rotadev.plan.application;

import static dev.rotadev.TestLogins.loggedAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.jayway.jsonpath.JsonPath;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.ai.AiResult;
import dev.rotadev.ai.CareerAiClient;
import dev.rotadev.ai.application.AiCallLog;
import dev.rotadev.ai.domain.TokenUsage;
import dev.rotadev.diagnosis.application.DiagnosisStore;
import dev.rotadev.diagnosis.domain.AxisAssessment;
import dev.rotadev.diagnosis.domain.DiagnosisResult;
import dev.rotadev.diagnosis.domain.Gap;
import dev.rotadev.goals.CareerGoal;
import dev.rotadev.goals.Level;
import dev.rotadev.goals.Track;
import dev.rotadev.goals.application.GoalService;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;
import dev.rotadev.jobs.application.JobWorker;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PlanFlowIntegrationTest {

    private static final String AI_PLAN = """
            {"summary": "Foco em testes primeiro.",
             "anchorProject": {"title": "Rota Dev", "description": "Evoluir o projeto", "deliverables": ["CI verde"], "evidenceIds": []},
             "quarters": [{"index": 1, "title": "Testes", "focus": "Testes de integração",
                           "milestones": [{"title": "Testcontainers no CI", "axisKey": "testing", "criteria": ["PR mergeado"]}],
                           "weeks": [{"index": 1, "theme": "Base", "tasks": [{"title": "Revisar diff", "kind": "REVIEW", "minutes": 60}]}]}],
             "resources": []}
            """;

    @MockitoBean
    CareerAiClient ai;

    @Autowired
    MockMvcTester mvc;

    @Autowired
    GithubSignIn signIn;

    @Autowired
    GoalService goals;

    @Autowired
    DiagnosisStore diagnoses;

    @Autowired
    AiCallLog aiCalls;

    @Autowired
    JobWorker worker;

    @Test
    void needsDiagnosisFirst() {
        User alice = user(12001L, "alice-plan");
        goals.save(goal(alice));

        assertThat(mvc.post().uri("/api/plan").with(loggedAs(alice)).with(csrf()))
                .hasStatus(HttpStatus.CONFLICT).bodyJson().extractingPath("$.detail").asString().contains("diagnóstico");
    }

    @Test
    void generatesVersionedPlan() {
        User bob = ready(user(12002L, "bob-plan"));
        when(ai.run(any())).thenAnswer(inv -> new AiResult(AI_PLAN, "claude-opus-5-5", aiCallFor(bob)));

        assertThat(mvc.post().uri("/api/plan").with(loggedAs(bob)).with(csrf())).hasStatus(HttpStatus.ACCEPTED)
                .bodyJson().extractingPath("$.version").isEqualTo(1);
        worker.runOnce(10);
        mvc.post().uri("/api/plan").with(loggedAs(bob)).with(csrf()).exchange();
        worker.runOnce(10);

        var current = assertThat(mvc.get().uri("/api/plan/current").with(loggedAs(bob))).hasStatusOk().bodyJson();
        current.extractingPath("$.version").isEqualTo(2);
        current.extractingPath("$.reason").isEqualTo("Plano refeito a pedido");
        current.extractingPath("$.content.quarters[0].milestones[0].axisKey").isEqualTo("testing");
        current.extractingPath("$.content.quarters[0].weeks[0].totalMinutes").isEqualTo(60);
        assertThat(mvc.get().uri("/api/plan/versions").with(loggedAs(bob))).hasStatusOk()
                .bodyJson().extractingPath("$.length()").isEqualTo(2);
    }

    /** Isolamento (ADR 0004): a versão do plano da Carol, pelo id, não existe para o Dave. */
    @Test
    void planOfAnotherUserIsNotFound() throws Exception {
        User carol = ready(user(12003L, "carol-plan"));
        User dave = user(12004L, "dave-plan");
        var created = mvc.post().uri("/api/plan").with(loggedAs(carol)).with(csrf()).exchange();
        String id = JsonPath.read(created.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");

        assertThat(mvc.get().uri("/api/plan/versions/{id}", id).with(loggedAs(carol))).hasStatusOk();
        assertThat(mvc.get().uri("/api/plan/versions/{id}", id).with(loggedAs(dave))).hasStatus(HttpStatus.NOT_FOUND);
    }

    /** Objetivo + diagnóstico pronto, gravados direto (o fluxo do diagnóstico tem o próprio teste). */
    private User ready(User user) {
        goals.save(goal(user));
        UUID id = diagnoses.insertPending(user.id(), "{}", Instant.now());
        diagnoses.markReady(user.id(), id, new DiagnosisResult("Resumo", "JUNIOR",
                List.of(new AxisAssessment("testing", "Testes", 3, 1, false, List.of(), "Sem testes")),
                List.of(new Gap("testing", "Testes", 2, 1, "Escrever testes"))), aiCallFor(user), Instant.now());
        return user;
    }

    private UUID aiCallFor(User user) {
        return aiCalls.record(new AiCallLog.Entry(user.id(), "test", 1, "claude-opus-5-5", 1, "SUCCEEDED", TokenUsage.NONE, 0L, 0, null, Instant.now()));
    }

    private static CareerGoal goal(User user) {
        return new CareerGoal(user.id(), "Backend Java", Level.PLENO, Track.BACKEND, LocalDate.now().plusMonths(8), 10, null, false, null);
    }

    private User user(long githubId, String login) {
        return signIn.signIn(new GithubProfile(githubId, login, null, null, null));
    }
}

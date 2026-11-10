package dev.rotadev.diagnosis.application;

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
import dev.rotadev.ai.AiCallFailedException;
import dev.rotadev.ai.AiResult;
import dev.rotadev.ai.CareerAiClient;
import dev.rotadev.ai.application.AiCallLog;
import dev.rotadev.ai.domain.TokenUsage;
import dev.rotadev.goals.CareerGoal;
import dev.rotadev.goals.Level;
import dev.rotadev.goals.Track;
import dev.rotadev.goals.application.GoalService;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;
import dev.rotadev.jobs.application.JobWorker;
import dev.rotadev.profile.Confidence;
import dev.rotadev.profile.Evidence;
import dev.rotadev.profile.ProfileReader;
import dev.rotadev.profile.ProfileRecorder;
import dev.rotadev.profile.SkillSignal;

/** Pedido → job → IA (falsa) → regras → tela, contra Postgres real. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DiagnosisFlowIntegrationTest {

    @MockitoBean
    CareerAiClient ai;

    @Autowired
    MockMvcTester mvc;

    @Autowired
    GithubSignIn signIn;

    @Autowired
    GoalService goals;

    @Autowired
    ProfileRecorder recorder;

    @Autowired
    ProfileReader profile;

    @Autowired
    JobWorker worker;

    @Autowired
    AiCallLog aiCalls;

    @Test
    void needsGoalAndProfileFirst() {
        User alice = user(11001L, "alice-diag");

        assertThat(mvc.post().uri("/api/diagnosis").with(loggedAs(alice)).with(csrf()))
                .hasStatus(HttpStatus.CONFLICT).bodyJson().extractingPath("$.detail").asString().contains("objetivo");
    }

    @Test
    void generatesDiagnosisCitingRealEvidence() {
        User bob = user(11002L, "bob-diag");
        UUID evidenceId = prepare(bob);
        when(ai.run(any())).thenReturn(new AiResult("""
                {"summary": "Base boa em Java, falta teste.", "estimatedLevel": "JUNIOR",
                 "axes": [{"key": "language", "score": 3, "evidenceIds": ["%s"], "rationale": "Java no rota-dev"},
                          {"key": "testing", "score": 4, "evidenceIds": ["%s"], "rationale": "Inventado"}],
                 "gaps": []}
                """.formatted(evidenceId, UUID.randomUUID()), "claude-opus-5-5", aiCallFor(bob)));

        assertThat(mvc.post().uri("/api/diagnosis").with(loggedAs(bob)).with(csrf())).hasStatus(HttpStatus.ACCEPTED);
        worker.runOnce(10);

        var body = assertThat(mvc.get().uri("/api/diagnosis/latest").with(loggedAs(bob))).hasStatusOk().bodyJson();
        body.extractingPath("$.status").isEqualTo("READY");
        body.extractingPath("$.axes[0].evidences[0].title").isEqualTo("rota-dev");
        body.extractingPath("$.axes[?(@.key == 'testing')].verified").isEqualTo(List.of(false));
        body.extractingPath("$.axes[?(@.key == 'testing')].score").isEqualTo(List.of(1));
        body.extractingPath("$.gaps[0].priority").isEqualTo(1);
    }

    @Test
    void refusalMarksFailedWithoutRetry() {
        User carol = user(11003L, "carol-diag");
        prepare(carol);
        when(ai.run(any())).thenThrow(new AiCallFailedException("a IA não completou a resposta (refusal)"));

        mvc.post().uri("/api/diagnosis").with(loggedAs(carol)).with(csrf()).exchange();
        worker.runOnce(10);

        assertThat(mvc.get().uri("/api/diagnosis/latest").with(loggedAs(carol)))
                .hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("FAILED");
    }

    /** Isolamento (ADR 0004): o diagnóstico do Dave, pelo id, não existe para a Erin. */
    @Test
    void diagnosisOfAnotherUserIsNotFound() throws Exception {
        User dave = user(11004L, "dave-diag");
        User erin = user(11005L, "erin-diag");
        prepare(dave);
        var created = mvc.post().uri("/api/diagnosis").with(loggedAs(dave)).with(csrf()).exchange();
        String id = JsonPath.read(created.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");

        assertThat(mvc.get().uri("/api/diagnosis/{id}", id).with(loggedAs(dave))).hasStatusOk();
        assertThat(mvc.get().uri("/api/diagnosis/{id}", id).with(loggedAs(erin))).hasStatus(HttpStatus.NOT_FOUND);
    }

    private UUID prepare(User user) {
        goals.save(new CareerGoal(user.id(), "Backend Java", Level.PLENO, Track.BACKEND, LocalDate.now().plusMonths(9), 10, null, false, null));
        recorder.record(user.id(), "GITHUB", UUID.randomUUID(), "{}", List.of(new SkillSignal("Java", 1, Confidence.HIGH)),
                List.of(new Evidence(null, "REPOSITORY", "Java", "rota-dev", "Java 100%", "https://github.com/x/rota-dev", Confidence.HIGH)));
        return profile.latestPerSource(user.id()).getFirst().evidences().getFirst().id();
    }

    /** O diagnóstico aponta para a linha da chamada em ai_calls (chave estrangeira): criamos uma de verdade. */
    private UUID aiCallFor(User user) {
        return aiCalls.record(new AiCallLog.Entry(user.id(), "diagnosis", 1, "claude-opus-5-5", 1, "SUCCEEDED",
                TokenUsage.NONE, 0L, 0, null, Instant.now()));
    }

    private User user(long githubId, String login) {
        return signIn.signIn(new GithubProfile(githubId, login, null, null, null));
    }
}

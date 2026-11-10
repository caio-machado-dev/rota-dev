package dev.rotadev.adjustment.web;

import static dev.rotadev.TestLogins.loggedAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.jayway.jsonpath.JsonPath;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.ai.CareerAiClient;
import dev.rotadev.ai.application.AiCallLog;
import dev.rotadev.ai.domain.TokenUsage;
import dev.rotadev.diagnosis.application.DiagnosisStore;
import dev.rotadev.diagnosis.domain.AxisAssessment;
import dev.rotadev.diagnosis.domain.DiagnosisResult;
import dev.rotadev.goals.CareerGoal;
import dev.rotadev.goals.Level;
import dev.rotadev.goals.Track;
import dev.rotadev.goals.application.GoalService;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;
import dev.rotadev.plan.application.PlanStore;
import dev.rotadev.plan.domain.PlanContent;

/** Check-in de domingo com semana difícil → proposta → aceitar gera a versão 2 do plano com o motivo. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, AdjustmentFlowTest.SundayClock.class})
class AdjustmentFlowTest {

    /** A versão nova do plano fica PENDING: o job não roda neste teste, e a IA nem é chamada. */
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
    PlanStore plans;

    @Autowired
    AiCallLog aiCalls;

    @Test
    void hardWeekProposalAcceptedCreatesNewPlanVersion() throws Exception {
        User alice = withPlan(user(16001L, "alice-adj"));
        checkin(alice, 2, "Mudança de casa");

        var list = assertThat(mvc.get().uri("/api/adjustments").with(loggedAs(alice))).hasStatusOk().bodyJson();
        list.extractingPath("$[0].kind").isEqualTo("LIGHTER_WEEK");
        list.extractingPath("$[0].status").isEqualTo("PROPOSED");
        String id = JsonPath.read(mvc.get().uri("/api/adjustments").with(loggedAs(alice)).exchange()
                .getResponse().getContentAsString(StandardCharsets.UTF_8), "$[0].id");

        assertThat(mvc.post().uri("/api/adjustments/{id}/accept", id).with(loggedAs(alice)).with(csrf())).hasStatus(HttpStatus.ACCEPTED);

        var latest = assertThat(mvc.get().uri("/api/plan/latest").with(loggedAs(alice))).hasStatusOk().bodyJson();
        latest.extractingPath("$.version").isEqualTo(2);
        latest.extractingPath("$.reason").asString().startsWith("Reajuste aceito no check-in de 09/11");
        // Já decidido: aceitar de novo não acha proposta pendente.
        assertThat(mvc.post().uri("/api/adjustments/{id}/accept", id).with(loggedAs(alice)).with(csrf())).hasStatus(HttpStatus.NOT_FOUND);
    }

    /** Isolamento (ADR 0004): o Bob não decide o reajuste da Carol. */
    @Test
    void cannotDecideAnotherUsersAdjustment() throws Exception {
        User carol = withPlan(user(16002L, "carol-adj"));
        User bob = user(16003L, "bob-adj");
        checkin(carol, 2, "Gripe");
        String id = JsonPath.read(mvc.get().uri("/api/adjustments").with(loggedAs(carol)).exchange()
                .getResponse().getContentAsString(StandardCharsets.UTF_8), "$[0].id");

        assertThat(mvc.post().uri("/api/adjustments/{id}/reject", id).with(loggedAs(bob)).with(csrf())).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.post().uri("/api/adjustments/{id}/reject", id).with(loggedAs(carol)).with(csrf())).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void noPlanNoProposal() {
        User dave = user(16004L, "dave-adj");
        checkin(dave, 1, "Sem plano ainda");

        assertThat(mvc.get().uri("/api/adjustments").with(loggedAs(dave))).hasStatusOk()
                .bodyJson().extractingPath("$.length()").isEqualTo(0);
    }

    private void checkin(User user, int energy, String blockers) {
        mvc.post().uri("/api/checkin").with(loggedAs(user)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"commits": 2, "energy": %d, "blockers": "%s"}
                        """.formatted(energy, blockers)).exchange();
    }

    /** Objetivo, diagnóstico pronto e plano v1 pronto, gravados direto pelos repositórios. */
    private User withPlan(User user) {
        Instant now = Instant.parse("2026-11-15T15:00:00Z");
        goals.save(new CareerGoal(user.id(), "Backend Java", Level.PLENO, Track.BACKEND, LocalDate.of(2027, 8, 31), 10, null, false, null));
        UUID callId = aiCalls.record(new AiCallLog.Entry(user.id(), "test", 1, "claude-opus-5-5", 1, "SUCCEEDED", TokenUsage.NONE, 0L, 0, null, now));
        UUID diagnosis = diagnoses.insertPending(user.id(), "{}", now);
        diagnoses.markReady(user.id(), diagnosis, new DiagnosisResult("", "JUNIOR",
                List.of(new AxisAssessment("testing", "Testes", 3, 1, false, List.of(), "")), List.of()), callId, now);
        UUID plan = plans.insertPending(user.id(), 1, "Primeira versão", diagnosis, LocalDate.of(2026, 11, 9), null, now);
        plans.markReady(user.id(), plan, new PlanContent("", new PlanContent.AnchorProject("", "", List.of(), List.of()),
                List.of(new PlanContent.Quarter(1, "T1", "", List.of(), List.of())), List.of()), callId, now);
        return user;
    }

    private User user(long githubId, String login) {
        return signIn.signIn(new GithubProfile(githubId, login, null, null, null));
    }

    @TestConfiguration
    static class SundayClock {

        @Bean
        @Primary
        Clock sundayClock() {
            return Clock.fixed(Instant.parse("2026-11-15T15:00:00Z"), ZoneOffset.UTC);
        }
    }
}

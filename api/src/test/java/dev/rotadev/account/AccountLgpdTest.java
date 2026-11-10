package dev.rotadev.account;

import static dev.rotadev.TestLogins.loggedAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.account.application.PersonalDataStore;
import dev.rotadev.ai.application.AiCallLog;
import dev.rotadev.ai.domain.TokenUsage;
import dev.rotadev.diagnosis.application.DiagnosisStore;
import dev.rotadev.diagnosis.domain.DiagnosisResult;
import dev.rotadev.goals.CareerGoal;
import dev.rotadev.goals.Level;
import dev.rotadev.goals.Track;
import dev.rotadev.goals.application.GoalService;
import dev.rotadev.identity.GithubAccountLinked;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;
import dev.rotadev.plan.application.PlanStore;
import dev.rotadev.plan.domain.PlanContent;
import dev.rotadev.sources.application.UploadDocument;
import dev.rotadev.sources.domain.UploadKind;

/** LGPD: exportar traz tudo sem segredos; excluir não deixa nenhuma linha, em nenhuma tabela, nem arquivo. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountLgpdTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    GithubSignIn signIn;

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    GoalService goals;

    @Autowired
    UploadDocument uploads;

    @Autowired
    AiCallLog aiCalls;

    @Autowired
    DiagnosisStore diagnoses;

    @Autowired
    PlanStore plans;

    @Autowired
    PersonalDataStore personalData;

    @Autowired
    JdbcClient jdbc;

    @Test
    void exportHasEveryTableButNoSecrets() throws Exception {
        User alice = richUser(17001L, "alice-lgpd");

        var result = mvc.get().uri("/api/account/export").with(loggedAs(alice)).exchange();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(result).hasStatusOk();
        assertThat(result.getResponse().getHeader("Content-Disposition")).isEqualTo("attachment; filename=\"rota-dev-dados.json\"");
        assertThat(body).contains("\"career_goals\"", "\"connected_sources\"", "\"plan_versions\"", "Backend Java")
                .doesNotContain("gho_segredo", "access_token_encrypted", "storage_key");
    }

    /** O critério da LGPD em forma de teste: depois de excluir, nenhuma tabela com user_id tem linha do usuário. */
    @Test
    void deleteLeavesNothingBehind() {
        User bob = richUser(17002L, "bob-lgpd");
        User carol = richUser(17003L, "carol-lgpd");

        assertThat(mvc.delete().uri("/api/account").with(loggedAs(bob)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmLogin\": \"login-errado\"}")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.delete().uri("/api/account").with(loggedAs(bob)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmLogin\": \"bob-lgpd\"}")).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(personalData.remainingRows(bob.id())).isZero();
        assertThat(Files.exists(Path.of("target/test-uploads", bob.id().toString()))).isFalse();
        // A Carol continua intacta.
        assertThat(personalData.remainingRows(carol.id())).isGreaterThan(5);
    }

    @Test
    void everyUserTableIsCoveredByTheCascade() {
        Set<String> tables = Set.copyOf(jdbc.sql("""
                SELECT table_name FROM information_schema.columns WHERE table_schema = 'public' AND column_name = 'user_id'
                """).query(String.class).list());

        assertThat(tables).contains("career_goals", "connected_sources", "study_sessions", "plan_versions", "readiness_scores", "ai_calls");
    }

    /** Usuário com dados em vários módulos: token, arquivo, objetivo, IA, diagnóstico, plano, sessão. */
    private User richUser(long githubId, String login) {
        User user = signIn.signIn(new GithubProfile(githubId, login, null, null, null));
        Instant now = Instant.now();
        events.publishEvent(new GithubAccountLinked(user.id(), login, "gho_segredo_" + login, Set.of()));
        uploads.upload(user.id(), UploadKind.RESUME, "cv.pdf", ("%PDF-1.7 " + login).getBytes(StandardCharsets.UTF_8));
        goals.save(new CareerGoal(user.id(), "Backend Java", Level.PLENO, Track.BACKEND, LocalDate.now().plusMonths(6), 8, null, false, null));
        UUID call = aiCalls.record(new AiCallLog.Entry(user.id(), "diagnosis", 1, "claude-opus-5-5", 1, "SUCCEEDED", TokenUsage.NONE, 0L, 0, null, now));
        UUID diagnosis = diagnoses.insertPending(user.id(), "{}", now);
        diagnoses.markReady(user.id(), diagnosis, new DiagnosisResult("", "JUNIOR", List.of(), List.of()), call, now);
        UUID plan = plans.insertPending(user.id(), 1, "Primeira versão", diagnosis, LocalDate.now(), null, now);
        plans.markReady(user.id(), plan, new PlanContent("", new PlanContent.AnchorProject("", "", List.of(), List.of()), List.of(), List.of()), call, now);
        mvc.post().uri("/api/tracking/sessions").with(loggedAs(user)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\": \"STUDY\", \"minutes\": 30, \"topic\": \"LGPD\"}").exchange();
        return user;
    }
}

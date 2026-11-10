package dev.rotadev.limits;

import static dev.rotadev.TestLogins.loggedAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.ai.application.AiCallLog;
import dev.rotadev.ai.domain.TokenUsage;
import dev.rotadev.goals.CareerGoal;
import dev.rotadev.goals.Level;
import dev.rotadev.goals.Track;
import dev.rotadev.goals.application.GoalService;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;
import dev.rotadev.profile.Confidence;
import dev.rotadev.profile.ProfileRecorder;
import dev.rotadev.profile.SkillSignal;
import dev.rotadev.sources.application.InvalidUploadException;
import dev.rotadev.sources.application.UploadDocument;
import dev.rotadev.sources.domain.UploadKind;

/** Limites por nível (rota.limits.*: FREE 10 gerações/mês e 10 arquivos; PRO 100). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TierLimitsTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    GithubSignIn signIn;

    @Autowired
    GoalService goals;

    @Autowired
    ProfileRecorder profile;

    @Autowired
    AiCallLog aiCalls;

    @Autowired
    UploadDocument uploads;

    @Autowired
    JdbcClient jdbc;

    @Test
    void freeUserIsStoppedAtTheMonthlyLimitBeforeAnyAiCall() {
        User alice = ready(user(18001L, "alice-lim"));
        generations(alice, 10);

        assertThat(mvc.post().uri("/api/diagnosis").with(loggedAs(alice)).with(csrf()))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .bodyJson().extractingPath("$.detail").asString().contains("10 gerações de IA do mês");
    }

    @Test
    void proUserHasMoreRoom() {
        User bob = ready(user(18002L, "bob-lim"));
        jdbc.sql("UPDATE users SET tier = 'PRO' WHERE id = :id").param("id", bob.id()).update();
        generations(bob, 10);

        assertThat(mvc.post().uri("/api/diagnosis").with(loggedAs(bob)).with(csrf())).hasStatus(HttpStatus.ACCEPTED);
        assertThat(mvc.get().uri("/api/me").with(loggedAs(bob))).hasStatusOk().bodyJson().extractingPath("$.tier").isEqualTo("PRO");
    }

    @Test
    void failedGenerationsDoNotCount() {
        User carol = ready(user(18003L, "carol-lim"));
        for (int i = 0; i < 12; i++) {
            aiCalls.record(new AiCallLog.Entry(carol.id(), "diagnosis", 1, "claude-opus-5-5", 1, "INVALID_OUTPUT", TokenUsage.NONE, 0L, 0, null, Instant.now()));
        }

        assertThat(mvc.post().uri("/api/diagnosis").with(loggedAs(carol)).with(csrf())).hasStatus(HttpStatus.ACCEPTED);
    }

    @Test
    void freeUserKeepsAtMostTenFiles() {
        User dave = user(18004L, "dave-lim");
        for (int i = 0; i < 10; i++) {
            uploads.upload(dave.id(), UploadKind.RESUME, "cv" + i + ".pdf", ("%PDF-1.7 versão " + i).getBytes(StandardCharsets.UTF_8));
        }

        assertThatThrownBy(() -> uploads.upload(dave.id(), UploadKind.RESUME, "cv-11.pdf", "%PDF-1.7 mais um".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidUploadException.class).hasMessageContaining("10 arquivos");
    }

    private void generations(User user, int count) {
        for (int i = 0; i < count; i++) {
            aiCalls.record(new AiCallLog.Entry(user.id(), "diagnosis", 1, "claude-opus-5-5", 1, "SUCCEEDED", TokenUsage.NONE, 0L, 0, null, Instant.now()));
        }
    }

    private User ready(User user) {
        goals.save(new CareerGoal(user.id(), "Backend", Level.PLENO, Track.BACKEND, LocalDate.now().plusMonths(6), 8, null, false, null));
        profile.record(user.id(), "GITHUB", UUID.randomUUID(), "{}", List.of(new SkillSignal("Java", 1, Confidence.HIGH)), List.of());
        return user;
    }

    private User user(long githubId, String login) {
        return signIn.signIn(new GithubProfile(githubId, login, null, null, null));
    }
}

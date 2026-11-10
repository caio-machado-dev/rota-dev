package dev.rotadev.goals.web;

import static dev.rotadev.TestLogins.loggedAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GoalControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    GithubSignIn signIn;

    @Test
    void savesAndReadsBackGoal() {
        User alice = user(9001L, "alice-goal");

        assertThat(put(alice, goalJson("Engenheiro Backend Java", LocalDate.now().plusMonths(9), 10))).hasStatusOk();

        var body = assertThat(mvc.get().uri("/api/goal").with(loggedAs(alice))).hasStatusOk().bodyJson();
        body.extractingPath("$.targetRole").isEqualTo("Engenheiro Backend Java");
        body.extractingPath("$.hoursPerWeek").isEqualTo(10);
        body.extractingPath("$.track").isEqualTo("BACKEND");
    }

    @Test
    void secondPutReplacesGoal() {
        User bob = user(9002L, "bob-goal");
        put(bob, goalJson("Backend", LocalDate.now().plusMonths(6), 8));

        put(bob, goalJson("Backend Pleno", LocalDate.now().plusMonths(12), 12));

        assertThat(mvc.get().uri("/api/goal").with(loggedAs(bob))).hasStatusOk()
                .bodyJson().extractingPath("$.targetRole").isEqualTo("Backend Pleno");
    }

    @Test
    void rejectsPastDeadlineAndOutOfRangeHours() {
        User carol = user(9003L, "carol-goal");

        assertThat(put(carol, goalJson("Backend", LocalDate.now().minusDays(1), 10)))
                .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.detail").isEqualTo("O prazo precisa ser uma data futura.");
        assertThat(put(carol, goalJson("Backend", LocalDate.now().plusMonths(3), 80))).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(put(carol, goalJson("", LocalDate.now().plusMonths(3), 10))).hasStatus(HttpStatus.BAD_REQUEST);
    }

    /** Isolamento (ADR 0004): o objetivo da Dave não aparece para o Erin. */
    @Test
    void goalIsPerUser() {
        User dave = user(9004L, "dave-goal");
        User erin = user(9005L, "erin-goal");
        put(dave, goalJson("Backend", LocalDate.now().plusMonths(6), 10));

        assertThat(mvc.get().uri("/api/goal").with(loggedAs(erin))).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void requiresCsrf() {
        User frank = user(9006L, "frank-goal");

        assertThat(mvc.put().uri("/api/goal").with(loggedAs(frank)).contentType(MediaType.APPLICATION_JSON)
                .content(goalJson("Backend", LocalDate.now().plusMonths(6), 10))).hasStatus(HttpStatus.FORBIDDEN);
    }

    private MvcTestResult put(User user, String json) {
        return mvc.put().uri("/api/goal").with(loggedAs(user)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static String goalJson(String role, LocalDate deadline, int hours) {
        return """
                {"targetRole": "%s", "targetLevel": "PLENO", "track": "BACKEND", "deadline": "%s",
                 "hoursPerWeek": %d, "primaryStack": "Java/Spring", "wantsCertifications": false, "notes": null}
                """.formatted(role, deadline, hours);
    }

    private User user(long githubId, String login) {
        return signIn.signIn(new GithubProfile(githubId, login, null, null, null));
    }
}

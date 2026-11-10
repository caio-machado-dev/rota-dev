package dev.rotadev.tracking.web;

import static dev.rotadev.TestLogins.loggedAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TrackingControllerTest {

    /** "Hoje" do usuário de teste: o fuso padrão é America/Sao_Paulo. */
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    @Autowired
    MockMvcTester mvc;

    @Autowired
    GithubSignIn signIn;

    @Test
    void loggedSessionShowsUpInNow() {
        User alice = user(13001L, "alice-trk");

        assertThat(post(alice, "/api/tracking/sessions", """
                {"kind": "STUDY", "minutes": 45, "topic": "Transações no Spring", "learning": "readOnly pula o dirty checking"}
                """)).hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("$.day").isEqualTo(TODAY.toString());

        var now = assertThat(mvc.get().uri("/api/now").with(loggedAs(alice))).hasStatusOk().bodyJson();
        now.extractingPath("$.minutesToday").isEqualTo(45);
        now.extractingPath("$.sessionsToday[0].topic").isEqualTo("Transações no Spring");
        now.extractingPath("$.planWeek").isNull();
    }

    @Test
    void rejectsFutureDayAndTooShortSession() {
        User bob = user(13002L, "bob-trk");

        assertThat(post(bob, "/api/tracking/sessions", """
                {"day": "%s", "kind": "STUDY", "minutes": 30, "topic": "Amanhã"}
                """.formatted(TODAY.plusDays(1)))).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(post(bob, "/api/tracking/sessions", """
                {"kind": "STUDY", "minutes": 2, "topic": "Rápido demais"}
                """)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    /** Isolamento (ADR 0004): a Erin não apaga a sessão da Dave; a resposta é 404. */
    @Test
    void cannotDeleteAnotherUsersSession() throws Exception {
        User dave = user(13003L, "dave-trk");
        User erin = user(13004L, "erin-trk");
        var created = post(dave, "/api/tracking/sessions", """
                {"kind": "BUILD", "minutes": 60, "topic": "Projeto-âncora"}
                """);
        String id = JsonPath.read(created.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");

        assertThat(mvc.delete().uri("/api/tracking/sessions/{id}", id).with(loggedAs(erin)).with(csrf())).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri("/api/tracking/sessions/{id}", id).with(loggedAs(dave)).with(csrf())).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void templateFeedsTodayAndRejectsDuplicates() {
        User frank = user(13005L, "frank-trk");
        int weekday = TODAY.getDayOfWeek().getValue();

        assertThat(put(frank, "/api/tracking/template", """
                {"slots": [{"weekday": %d, "kind": "REVIEW", "minutes": 30}, {"weekday": %d, "kind": "REVIEW", "minutes": 20}]}
                """.formatted(weekday, weekday))).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(put(frank, "/api/tracking/template", """
                {"slots": [{"weekday": %d, "kind": "REVIEW", "minutes": 30}]}
                """.formatted(weekday))).hasStatusOk();

        assertThat(mvc.get().uri("/api/now").with(loggedAs(frank))).hasStatusOk()
                .bodyJson().extractingPath("$.templateToday[0].minutes").isEqualTo(30);
    }

    @Test
    void reviewHabitIsUpsertedPerDay() {
        User gina = user(13006L, "gina-trk");

        put(gina, "/api/tracking/review-habit", """
                {"day": "%s", "readDiff": true, "explained": false, "tested": false}
                """.formatted(TODAY));
        assertThat(put(gina, "/api/tracking/review-habit", """
                {"day": "%s", "readDiff": true, "explained": true, "tested": true, "note": "SKIP LOCKED evita dois workers no mesmo job"}
                """.formatted(TODAY))).hasStatusOk().bodyJson().extractingPath("$.complete").isEqualTo(true);

        assertThat(mvc.get().uri("/api/now").with(loggedAs(gina))).hasStatusOk()
                .bodyJson().extractingPath("$.habitToday.stepsDone").isEqualTo(4);
    }

    private MvcTestResult post(User user, String uri, String json) {
        return mvc.post().uri(uri).with(loggedAs(user)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private MvcTestResult put(User user, String uri, String json) {
        return mvc.put().uri(uri).with(loggedAs(user)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private User user(long githubId, String login) {
        return signIn.signIn(new GithubProfile(githubId, login, null, null, null));
    }
}

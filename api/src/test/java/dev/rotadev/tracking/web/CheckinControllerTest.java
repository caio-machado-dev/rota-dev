package dev.rotadev.tracking.web;

import static dev.rotadev.TestLogins.loggedAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;
import dev.rotadev.tracking.CheckinCompleted;

/**
 * Aqui o relógio é fixo num domingo (15/11/2026, meio-dia em São Paulo). É por isso que o código
 * pede um {@link Clock} em vez de chamar {@code Instant.now()}: o teste escolhe o dia.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, CheckinControllerTest.SundayClock.class})
class CheckinControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    GithubSignIn signIn;

    @Autowired
    SundayClock events;

    @Test
    void sundayCheckinFreezesTheWeekNumbersAndPublishesEvent() {
        User alice = user(14001L, "alice-chk");
        post(alice, "/api/tracking/sessions", """
                {"kind": "BUILD", "minutes": 120, "topic": "Projeto-âncora"}
                """);
        put(alice, "/api/tracking/review-habit", """
                {"day": "2026-11-15", "readDiff": true, "explained": true, "tested": true, "note": "Entendi o SKIP LOCKED"}
                """);

        var status = assertThat(mvc.get().uri("/api/checkin").with(loggedAs(alice))).hasStatusOk().bodyJson();
        status.extractingPath("$.openWeek").isEqualTo("2026-11-09");
        status.extractingPath("$.minutesLogged").isEqualTo(120);

        assertThat(post(alice, "/api/checkin", """
                {"commits": 12, "energy": 4, "blockers": "Pouco tempo na quinta"}
                """)).hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("$.reviewDays").isEqualTo(1);
        assertThat(events.received).contains(new CheckinCompleted(alice.id(), LocalDate.of(2026, 11, 9)));

        assertThat(post(alice, "/api/checkin", """
                {"commits": 1, "energy": 3}
                """)).hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.detail").asString().contains("já foi feito");
    }

    @Test
    void milestoneRequiresCurrentPlan() {
        User bob = user(14002L, "bob-chk");

        assertThat(post(bob, "/api/milestones", """
                {"milestone": "Testcontainers no CI", "evidenceUrl": "https://github.com/x/pull/1"}
                """)).hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.detail").isEqualTo("Sem plano em vigor.");
    }

    /** Isolamento (ADR 0004): o histórico de check-in é só do dono. */
    @Test
    void historyIsPerUser() {
        User carol = user(14003L, "carol-chk");
        User dave = user(14004L, "dave-chk");
        post(carol, "/api/checkin", """
                {"commits": 3, "energy": 3}
                """);

        assertThat(mvc.get().uri("/api/checkin/history").with(loggedAs(carol))).hasStatusOk()
                .bodyJson().extractingPath("$.length()").isEqualTo(1);
        assertThat(mvc.get().uri("/api/checkin/history").with(loggedAs(dave))).hasStatusOk()
                .bodyJson().extractingPath("$.length()").isEqualTo(0);
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

    @TestConfiguration
    static class SundayClock {

        final List<CheckinCompleted> received = new CopyOnWriteArrayList<>();

        /** {@code @Primary}: entre os dois {@link Clock} do contexto, este ganha. */
        @Bean
        @Primary
        Clock sundayClock() {
            return Clock.fixed(Instant.parse("2026-11-15T15:00:00Z"), ZoneOffset.UTC);
        }

        @EventListener
        void on(CheckinCompleted event) {
            received.add(event);
        }
    }
}

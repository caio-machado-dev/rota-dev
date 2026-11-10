package dev.rotadev.readiness.web;

import static dev.rotadev.TestLogins.loggedAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;

/** Check-in num domingo grava um ponto do índice; o índice atual reflete as sessões. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ReadinessControllerTest.SundayClock.class})
class ReadinessControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    GithubSignIn signIn;

    @Test
    void checkinRecordsAPointInHistory() {
        User alice = signIn.signIn(new GithubProfile(15001L, "alice-rdy", null, null, null));
        mvc.put().uri("/api/tracking/review-habit").with(loggedAs(alice)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"day": "2026-11-15", "readDiff": true, "explained": true, "tested": true, "note": "ok"}
                        """).exchange();

        mvc.post().uri("/api/checkin").with(loggedAs(alice)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"commits": 5, "energy": 4}
                        """).exchange();

        var body = assertThat(mvc.get().uri("/api/readiness").with(loggedAs(alice))).hasStatusOk().bodyJson();
        // Só o hábito conta (sem diagnóstico, plano nem objetivo): 1 dia de 20 = 5% × 15 = 0,75 → 1.
        body.extractingPath("$.current.habit").isEqualTo(5);
        body.extractingPath("$.current.score").isEqualTo(1);
        body.extractingPath("$.history[0].weekStart").isEqualTo("2026-11-09");
        body.extractingPath("$.history[0].value.score").isEqualTo(1);
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

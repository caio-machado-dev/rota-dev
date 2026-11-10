package dev.rotadev.sources.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.identity.GithubAccountLinked;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class GithubConnectionIntegrationTest {

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    SourceRepository sources;

    @Autowired
    GithubSignIn signIn;

    @Autowired
    JdbcClient jdbc;

    @Test
    void storesTokenEncryptedAndReadsItBack() {
        UUID userId = signIn.signIn(new GithubProfile(3001L, "cifra", null, null, null)).id();

        events.publishEvent(new GithubAccountLinked(userId, "cifra", "gho_segredo123", Set.of("read:user")));

        String stored = jdbc.sql("SELECT access_token_encrypted FROM connected_sources WHERE user_id = :id")
                .param("id", userId).query(String.class).single();
        assertThat(stored).startsWith("v1:").doesNotContain("gho_segredo123");
        assertThat(sources.findGithubToken(userId)).contains("gho_segredo123");
    }

    @Test
    void secondLoginReplacesTokenWithoutDuplicating() {
        UUID userId = signIn.signIn(new GithubProfile(3002L, "relogin", null, null, null)).id();

        events.publishEvent(new GithubAccountLinked(userId, "relogin", "gho_antigo", Set.of()));
        events.publishEvent(new GithubAccountLinked(userId, "relogin", "gho_novo", Set.of()));

        assertThat(sources.findAllByUser(userId)).hasSize(1);
        assertThat(sources.findGithubToken(userId)).contains("gho_novo");
    }

    @Test
    void eventToStringHidesToken() {
        assertThat(new GithubAccountLinked(UUID.randomUUID(), "x", "gho_nao_aparece", Set.of()).toString())
                .doesNotContain("gho_nao_aparece");
    }
}

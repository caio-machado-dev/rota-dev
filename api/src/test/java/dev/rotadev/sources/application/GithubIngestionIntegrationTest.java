package dev.rotadev.sources.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.identity.GithubAccountLinked;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.jobs.application.JobWorker;
import dev.rotadev.profile.ProfileReader;
import dev.rotadev.sources.domain.GithubRepo;

/**
 * O caminho inteiro: login → token guardado → job enfileirado → worker → snapshot de perfil.
 * Só a API do GitHub é falsa: {@code @MockitoBean} troca o bean {@link GithubApi} por um mock.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class GithubIngestionIntegrationTest {

    @MockitoBean
    GithubApi github;

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    GithubSignIn signIn;

    @Autowired
    JobWorker worker;

    @Autowired
    ProfileReader profile;

    @Autowired
    SourceQueries sources;

    @Test
    void loginTriggersIngestionThatRecordsSnapshot() {
        UUID userId = signIn.signIn(new GithubProfile(6001L, "ingest", null, null, null)).id();
        when(github.recentOwnedRepos(eq("gho_ingest"), anyInt())).thenReturn(List.of(new GithubRepo(
                "rota-dev", "ingest/rota-dev", "https://github.com/ingest/rota-dev", null, false, false, 1,
                Instant.parse("2026-11-09T00:00:00Z"), Map.of("Java", 900L, "TypeScript", 100L), true)));

        events.publishEvent(new GithubAccountLinked(userId, "ingest", "gho_ingest", Set.of("read:user")));
        worker.runOnce(10);

        assertThat(profile.latestPerSource(userId)).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.sourceType()).isEqualTo("GITHUB");
            assertThat(snapshot.skills().getFirst().name()).isEqualTo("Java");
            assertThat(snapshot.evidences()).singleElement()
                    .satisfies(e -> assertThat(e.detail()).contains("CI com GitHub Actions"));
        });
        assertThat(sources.listFor(userId)).singleElement()
                .satisfies(s -> assertThat(s.lastSyncedAt()).isNotNull());
    }
}

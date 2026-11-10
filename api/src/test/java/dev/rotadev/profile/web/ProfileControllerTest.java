package dev.rotadev.profile.web;

import static dev.rotadev.TestLogins.loggedAs;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;
import dev.rotadev.profile.Confidence;
import dev.rotadev.profile.Evidence;
import dev.rotadev.profile.ProfileReader;
import dev.rotadev.profile.ProfileRecorder;
import dev.rotadev.profile.SkillSignal;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProfileControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    GithubSignIn signIn;

    @Autowired
    ProfileRecorder recorder;

    @Autowired
    ProfileReader reader;

    private static final List<SkillSignal> SKILLS = List.of(new SkillSignal("Java", 0.7, Confidence.HIGH));
    private static final List<Evidence> EVIDENCES = List.of(new Evidence(
            null, "REPOSITORY", "Java", "rota-dev", "Java 70%, CI com GitHub Actions", "https://github.com/x/rota-dev", Confidence.HIGH));

    @Test
    void showsLatestSnapshotWithSkillsAndEvidences() {
        User alice = user(5001L, "alice-prof");
        recorder.record(alice.id(), "GITHUB", UUID.randomUUID(), "{}", List.of(new SkillSignal("Python", 1, Confidence.HIGH)), List.of());
        recorder.record(alice.id(), "GITHUB", UUID.randomUUID(), "{}", SKILLS, EVIDENCES);

        var response = assertThat(mvc.get().uri("/api/profile").with(loggedAs(alice))).hasStatusOk().bodyJson();
        response.extractingPath("$.snapshots.length()").isEqualTo(1);
        response.extractingPath("$.snapshots[0].skills[0].name").isEqualTo("Java");
        response.extractingPath("$.snapshots[0].evidences[0].url").isEqualTo("https://github.com/x/rota-dev");
    }

    /** O teste de isolamento do AGENTS.md: A tenta ler um recurso de B pelo id e recebe 404. */
    @Test
    void snapshotOfAnotherUserIsNotFound() {
        User alice = user(5002L, "alice-iso");
        User bob = user(5003L, "bob-iso");
        recorder.record(alice.id(), "GITHUB", UUID.randomUUID(), "{}", SKILLS, EVIDENCES);
        UUID aliceSnapshot = reader.latestPerSource(alice.id()).getFirst().id();

        assertThat(mvc.get().uri("/api/profile/snapshots/{id}", aliceSnapshot).with(loggedAs(alice))).hasStatusOk();
        assertThat(mvc.get().uri("/api/profile/snapshots/{id}", aliceSnapshot).with(loggedAs(bob))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/profile").with(loggedAs(bob)))
                .hasStatusOk().bodyJson().extractingPath("$.snapshots").isEqualTo(List.of());
    }

    @Test
    void sameJobRecordsOnlyOnce() {
        User carol = user(5004L, "carol-prof");
        UUID jobId = UUID.randomUUID();

        assertThat(recorder.record(carol.id(), "GITHUB", jobId, "{}", SKILLS, EVIDENCES)).isTrue();
        assertThat(recorder.record(carol.id(), "GITHUB", jobId, "{}", SKILLS, EVIDENCES)).isFalse();

        assertThat(reader.latestPerSource(carol.id())).singleElement()
                .satisfies(s -> assertThat(s.evidences()).hasSize(1));
    }

    @Test
    void requiresSession() {
        assertThat(mvc.get().uri("/api/profile")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private User user(long githubId, String login) {
        return signIn.signIn(new GithubProfile(githubId, login, null, null, null));
    }
}

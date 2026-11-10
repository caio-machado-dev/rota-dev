package dev.rotadev.sources.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.identity.AuthenticatedUser;
import dev.rotadev.identity.GithubAccountLinked;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SourcesControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    GithubSignIn signIn;

    @Autowired
    ApplicationEventPublisher events;

    @Test
    void requiresSession() {
        assertThat(mvc.get().uri("/api/sources")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /** Isolamento (ADR 0004): Bob não vê a fonte da Alice, e nenhuma resposta traz o token. */
    @Test
    void listsOnlyOwnSourcesAndNeverTheToken() {
        User alice = signIn.signIn(new GithubProfile(4001L, "alice-src", null, null, null));
        User bob = signIn.signIn(new GithubProfile(4002L, "bob-src", null, null, null));
        events.publishEvent(new GithubAccountLinked(alice.id(), "alice-src", "gho_da_alice", Set.of()));

        var asAlice = assertThat(mvc.get().uri("/api/sources").with(loggedAs(alice))).hasStatusOk();
        asAlice.bodyJson().extractingPath("$[0].type").isEqualTo("GITHUB");
        asAlice.bodyJson().extractingPath("$[0].externalLogin").isEqualTo("alice-src");
        asAlice.body().asString().doesNotContain("gho_da_alice");

        assertThat(mvc.get().uri("/api/sources").with(loggedAs(bob)))
                .hasStatusOk().bodyJson().isEqualTo("[]");
    }

    @Test
    void syncAcceptsWhenGithubIsConnectedAndNotFoundOtherwise() {
        User erin = signIn.signIn(new GithubProfile(4003L, "erin-src", null, null, null));
        User frank = signIn.signIn(new GithubProfile(4004L, "frank-src", null, null, null));
        events.publishEvent(new GithubAccountLinked(erin.id(), "erin-src", "gho_erin", Set.of()));

        assertThat(mvc.post().uri("/api/sources/github/sync").with(loggedAs(erin))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post().uri("/api/sources/github/sync").with(loggedAs(erin)).with(csrf())).hasStatus(HttpStatus.ACCEPTED);
        assertThat(mvc.post().uri("/api/sources/github/sync").with(loggedAs(frank)).with(csrf())).hasStatus(HttpStatus.NOT_FOUND);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor loggedAs(User user) {
        return oauth2Login().oauth2User(new AuthenticatedUser(user.id(), user.login()));
    }
}

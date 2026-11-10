package dev.rotadev.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.identity.AuthenticatedUser;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;

/**
 * {@code oauth2Login().oauth2User(...)} coloca um usuário logado na requisição sem passar pelo
 * GitHub: é o atalho de teste do Spring Security, como o {@code client.force_login} do Django.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MeControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    GithubSignIn signIn;

    @Test
    void requiresSession() {
        assertThat(mvc.get().uri("/api/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /** Isolamento (ADR 0004): cada sessão só enxerga o próprio usuário. */
    @Test
    void eachUserSeesOnlyThemselves() {
        User alice = signIn.signIn(new GithubProfile(1001L, "alice", "Alice", null, null));
        User bob = signIn.signIn(new GithubProfile(1002L, "bob", "Bob", null, null));

        assertThat(mvc.get().uri("/api/me").with(loggedAs(alice)))
                .hasStatusOk().bodyJson().extractingPath("$.login").isEqualTo("alice");
        assertThat(mvc.get().uri("/api/me").with(loggedAs(bob)))
                .hasStatusOk().bodyJson().extractingPath("$.login").isEqualTo("bob");
    }

    @Test
    void returnsTimeZone() {
        User carol = signIn.signIn(new GithubProfile(1003L, "carol", null, null, null));

        assertThat(mvc.get().uri("/api/me").with(loggedAs(carol)))
                .hasStatusOk().bodyJson().extractingPath("$.timeZone").isEqualTo("America/Sao_Paulo");
    }

    @Test
    void logoutRequiresCsrfToken() {
        User dave = signIn.signIn(new GithubProfile(1004L, "dave", null, null, null));

        assertThat(mvc.post().uri("/api/logout").with(loggedAs(dave))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post().uri("/api/logout").with(loggedAs(dave)).with(csrf())).hasStatus(HttpStatus.NO_CONTENT);
    }

    /** O front lê este cookie para mandar o cabeçalho X-XSRF-TOKEN (web/src/lib/api/client.ts). */
    @Test
    void sessionRequestHandsOutCsrfCookie() {
        User erin = signIn.signIn(new GithubProfile(1005L, "erin", null, null, null));

        assertThat(mvc.get().uri("/api/me").with(loggedAs(erin)))
                .hasStatusOk()
                .cookies().containsCookie("XSRF-TOKEN");
    }

    @Test
    void loginStartsAtApiPrefix() {
        assertThat(mvc.get().uri("/api/oauth2/authorization/github"))
                .hasStatus(HttpStatus.FOUND)
                .redirectedUrl().startsWith("https://github.com/login/oauth/authorize");
    }

    private static RequestPostProcessor loggedAs(User user) {
        return oauth2Login().oauth2User(new AuthenticatedUser(user.id(), user.login()));
    }
}

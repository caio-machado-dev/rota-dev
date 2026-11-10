package dev.rotadev.sources.web;

import static dev.rotadev.TestLogins.loggedAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UploadsControllerTest {

    private static final byte[] PDF = "%PDF-1.7\nconteúdo de teste".getBytes(StandardCharsets.UTF_8);

    @Autowired
    MockMvcTester mvc;

    @Autowired
    GithubSignIn signIn;

    @Test
    void acceptsResumePdfAndDeduplicatesSameFile() {
        User alice = user(7001L, "alice-up");

        var first = assertThat(upload(alice, "RESUME", "cv.pdf", PDF)).hasStatus(HttpStatus.ACCEPTED);
        first.bodyJson().extractingPath("$.format").isEqualTo("PDF");
        first.bodyJson().extractingPath("$.status").isEqualTo("RECEIVED");
        assertThat(upload(alice, "RESUME", "cv-de-novo.pdf", PDF)).hasStatus(HttpStatus.ACCEPTED)
                .bodyJson().extractingPath("$.originalName").isEqualTo("cv.pdf");

        assertThat(mvc.get().uri("/api/sources/uploads").with(loggedAs(alice)))
                .hasStatusOk().bodyJson().extractingPath("$.length()").isEqualTo(1);
    }

    @Test
    void rejectsWrongFormatWithProblemDetail() {
        User bob = user(7002L, "bob-up");

        assertThat(upload(bob, "RESUME", "cv.pdf", "<html>".getBytes(StandardCharsets.UTF_8)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").asString().contains("Formato não reconhecido");
    }

    @Test
    void requiresCsrf() {
        User carol = user(7003L, "carol-up");
        var file = new MockMultipartFile("file", "cv.pdf", "application/pdf", PDF);

        assertThat(mvc.post().uri("/api/sources/uploads").multipart().file(file).param("kind", "RESUME").with(loggedAs(carol)))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    /** Isolamento (ADR 0004): a lista de arquivos é só do dono. */
    @Test
    void listsOnlyOwnUploads() {
        User dave = user(7004L, "dave-up");
        User erin = user(7005L, "erin-up");
        upload(dave, "RESUME", "cv.pdf", "%PDF-1.7 dave".getBytes(StandardCharsets.UTF_8));

        assertThat(mvc.get().uri("/api/sources/uploads").with(loggedAs(erin)))
                .hasStatusOk().bodyJson().extractingPath("$.length()").isEqualTo(0);
    }

    private org.springframework.test.web.servlet.assertj.MvcTestResult upload(User user, String kind, String name, byte[] content) {
        var file = new MockMultipartFile("file", name, "application/octet-stream", content);
        return mvc.post().uri("/api/sources/uploads").multipart().file(file).param("kind", kind)
                .with(loggedAs(user)).with(csrf()).exchange();
    }

    private User user(long githubId, String login) {
        return signIn.signIn(new GithubProfile(githubId, login, null, null, null));
    }
}

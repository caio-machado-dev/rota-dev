package dev.rotadev.sources.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.jobs.application.JobWorker;
import dev.rotadev.profile.ProfileReader;
import dev.rotadev.sources.domain.UploadKind;
import dev.rotadev.sources.infrastructure.TestDocuments;

/** Upload → job → leitura do arquivo → snapshot, contra Postgres real e o disco de teste. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DocumentParsingIntegrationTest {

    @Autowired
    UploadDocument upload;

    @Autowired
    JobWorker worker;

    @Autowired
    ProfileReader profile;

    @Autowired
    SourceFileRepository files;

    @Autowired
    GithubSignIn signIn;

    @Autowired
    JdbcClient jdbc;

    @Test
    void resumePdfBecomesResumeSnapshot() throws Exception {
        UUID userId = signIn.signIn(new GithubProfile(8001L, "cv-user", null, null, null)).id();

        var file = upload.upload(userId, UploadKind.RESUME, "cv.pdf", TestDocuments.pdf("Backend Python, Django e PostgreSQL"));
        worker.runOnce(10);

        assertThat(profile.latestPerSource(userId)).singleElement().satisfies(s -> {
            assertThat(s.sourceType()).isEqualTo("RESUME");
            assertThat(s.skills()).extracting(sk -> sk.name()).contains("Python", "Django", "PostgreSQL");
        });
        assertThat(files.findById(userId, file.id())).get().extracting(f -> f.status()).isEqualTo("PROCESSED");
    }

    /** PDF quebrado: o arquivo fica FAILED e o job termina, sem ficar tentando de novo. */
    @Test
    void brokenPdfIsMarkedFailedWithoutRetry() {
        UUID userId = signIn.signIn(new GithubProfile(8002L, "cv-broken", null, null, null)).id();

        var file = upload.upload(userId, UploadKind.RESUME, "cv.pdf", "%PDF-1.7 só o cabeçalho".getBytes(StandardCharsets.UTF_8));
        worker.runOnce(10);

        assertThat(files.findById(userId, file.id())).get().extracting(f -> f.status()).isEqualTo("FAILED");
        assertThat(jdbc.sql("SELECT status FROM jobs WHERE idempotency_key = :k").param("k", "document.parse:" + file.id())
                .query(String.class).single()).isEqualTo("SUCCEEDED");
        assertThat(profile.latestPerSource(userId)).isEmpty();
    }
}

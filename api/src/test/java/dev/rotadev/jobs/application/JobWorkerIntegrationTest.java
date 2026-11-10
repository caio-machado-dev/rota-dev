package dev.rotadev.jobs.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.jobs.Job;
import dev.rotadev.jobs.JobHandler;
import dev.rotadev.jobs.JobQueue;

/** A fila inteira contra um Postgres real: enfileirar, pegar, executar, falhar e reagendar. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, JobWorkerIntegrationTest.Handlers.class})
class JobWorkerIntegrationTest {

    @Autowired
    JobQueue queue;

    @Autowired
    JobWorker worker;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    GithubSignIn signIn;

    @Autowired
    Handlers handlers;

    UUID userId;

    @BeforeEach
    void setUp() {
        jdbc.sql("DELETE FROM jobs").update();
        handlers.received.clear();
        userId = signIn.signIn(new GithubProfile(2001L, "jobs-user", null, null, null)).id();
    }

    @Test
    void runsJobAndMarksSucceeded() {
        queue.enqueue(userId, "test.ok", "{\"repo\": \"rota-dev\"}", "ok-1");

        assertThat(worker.runOnce(10)).isEqualTo(1);

        assertThat(handlers.received).singleElement().satisfies(job -> {
            assertThat(job.userId()).isEqualTo(userId);
            assertThat(job.payload()).contains("rota-dev");
            assertThat(job.attempts()).isEqualTo(1);
        });
        assertThat(statusOf("ok-1")).isEqualTo("SUCCEEDED");
        assertThat(worker.runOnce(10)).isZero();
    }

    @Test
    void sameIdempotencyKeyCreatesOneJob() {
        queue.enqueue(userId, "test.ok", "{}", "same-key");
        queue.enqueue(userId, "test.ok", "{}", "same-key");

        assertThat(jdbc.sql("SELECT count(*) FROM jobs").query(Long.class).single()).isEqualTo(1L);
    }

    @Test
    void failureReschedulesWithBackoffAndKeepsError() {
        queue.enqueue(userId, "test.fail", "{}", "fail-1");

        worker.runOnce(10);

        assertThat(statusOf("fail-1")).isEqualTo("PENDING");
        Instant runAfter = jdbc.sql("SELECT run_after FROM jobs WHERE idempotency_key = 'fail-1'")
                .query(Instant.class).single();
        assertThat(runAfter).isAfter(Instant.now().plusSeconds(20));
        assertThat(jdbc.sql("SELECT last_error FROM jobs WHERE idempotency_key = 'fail-1'").query(String.class).single())
                .contains("sempre falha");
        // Ainda não chegou a hora da próxima tentativa.
        assertThat(worker.runOnce(10)).isZero();
    }

    @Test
    void failsForGoodAfterMaxAttempts() {
        queue.enqueue(userId, "test.fail", "{}", "fail-max");

        for (int attempt = 1; attempt <= 6; attempt++) {
            // Simula a passagem do tempo: a próxima tentativa já está liberada.
            jdbc.sql("UPDATE jobs SET run_after = now() - interval '1 second' WHERE idempotency_key = 'fail-max'").update();
            worker.runOnce(10);
        }

        assertThat(statusOf("fail-max")).isEqualTo("FAILED");
        assertThat(jdbc.sql("SELECT attempts FROM jobs WHERE idempotency_key = 'fail-max'").query(Integer.class).single())
                .isEqualTo(6);
    }

    @Test
    void unknownTypeFailsImmediately() {
        queue.enqueue(userId, "tipo.que.nao.existe", "{}", "unknown-1");

        worker.runOnce(10);

        assertThat(statusOf("unknown-1")).isEqualTo("FAILED");
    }

    @Test
    void reclaimsJobStuckInRunning() {
        queue.enqueue(userId, "test.ok", "{}", "stuck-1");
        // Um worker pegou o job há uma hora e morreu antes de terminar.
        jdbc.sql("UPDATE jobs SET status = 'RUNNING', attempts = 1, locked_at = now() - interval '1 hour' WHERE idempotency_key = 'stuck-1'")
                .update();

        worker.runOnce(10);

        assertThat(statusOf("stuck-1")).isEqualTo("SUCCEEDED");
        assertThat(handlers.received).singleElement().extracting(Job::attempts).isEqualTo(2);
    }

    private String statusOf(String key) {
        return jdbc.sql("SELECT status FROM jobs WHERE idempotency_key = :key").param("key", key).query(String.class).single();
    }

    @TestConfiguration
    static class Handlers {

        final List<Job> received = new CopyOnWriteArrayList<>();

        @Bean
        JobHandler okHandler() {
            return new JobHandler() {
                @Override
                public String type() {
                    return "test.ok";
                }

                @Override
                public void handle(Job job) {
                    received.add(job);
                }
            };
        }

        @Bean
        JobHandler failingHandler() {
            return new JobHandler() {
                @Override
                public String type() {
                    return "test.fail";
                }

                @Override
                public void handle(Job job) {
                    throw new IllegalStateException("sempre falha");
                }
            };
        }
    }
}

package dev.rotadev.jobs.infrastructure;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import dev.rotadev.jobs.Job;
import dev.rotadev.jobs.JobQueue;
import dev.rotadev.jobs.application.JobStore;
import dev.rotadev.shared.UuidV7;

/**
 * Fila de jobs em SQL direto, com o {@link JdbcClient} do Spring.
 *
 * <p>Aqui não usamos JPA: o que importa é o SQL exato ({@code FOR UPDATE SKIP LOCKED},
 * {@code ON CONFLICT}, {@code RETURNING}), e o JdbcClient deixa esse SQL à vista, como um
 * {@code cursor.execute(...)} com parâmetros nomeados.
 */
@Repository
class JdbcJobStore implements JobQueue, JobStore {

    /** Tentativas por job. Com o backoff do worker, a última acontece umas 2 horas depois da primeira. */
    static final int MAX_ATTEMPTS = 6;

    private final JdbcClient jdbc;
    private final Clock clock;

    JdbcJobStore(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public void enqueue(UUID userId, String type, String payloadJson, String idempotencyKey) {
        Instant now = clock.instant();
        jdbc.sql("""
                INSERT INTO jobs (id, user_id, type, payload, status, max_attempts, run_after,
                                  idempotency_key, created_at, updated_at)
                VALUES (:id, :userId, :type, CAST(:payload AS jsonb), 'PENDING', :maxAttempts, :now,
                        :key, :now, :now)
                ON CONFLICT (idempotency_key) DO NOTHING
                """)
                .param("id", UuidV7.generate(now))
                .param("userId", userId)
                .param("type", type)
                .param("payload", payloadJson)
                .param("maxAttempts", MAX_ATTEMPTS)
                .param("now", Timestamp.from(now))
                .param("key", idempotencyKey)
                .update();
    }

    /**
     * Um único comando: o SELECT interno trava as linhas escolhidas ({@code FOR UPDATE}) e pula as
     * que outra transação já travou ({@code SKIP LOCKED}); o UPDATE marca como RUNNING e o
     * {@code RETURNING} devolve as linhas alteradas. Por ser um comando só, é atômico sem
     * precisar de {@code @Transactional}.
     */
    @Override
    public List<ClaimedJob> claim(Instant now, Instant staleBefore, int limit) {
        return jdbc.sql("""
                UPDATE jobs
                   SET status = 'RUNNING', attempts = attempts + 1, locked_at = :now, updated_at = :now
                 WHERE id IN (SELECT id FROM jobs
                               WHERE (status = 'PENDING' AND run_after <= :now)
                                  OR (status = 'RUNNING' AND locked_at < :staleBefore)
                               ORDER BY run_after
                               LIMIT :limit
                               FOR UPDATE SKIP LOCKED)
                RETURNING id, user_id, type, payload::text AS payload, attempts, max_attempts
                """)
                .param("now", Timestamp.from(now))
                .param("staleBefore", Timestamp.from(staleBefore))
                .param("limit", limit)
                .query((rs, row) -> new ClaimedJob(
                        new Job(
                                rs.getObject("id", UUID.class),
                                rs.getObject("user_id", UUID.class),
                                rs.getString("type"),
                                rs.getString("payload"),
                                rs.getInt("attempts")),
                        rs.getInt("max_attempts")))
                .list();
    }

    @Override
    public void markSucceeded(UUID id, Instant now) {
        jdbc.sql("UPDATE jobs SET status = 'SUCCEEDED', locked_at = NULL, last_error = NULL, updated_at = :now WHERE id = :id")
                .param("now", Timestamp.from(now))
                .param("id", id)
                .update();
    }

    @Override
    public void reschedule(UUID id, Instant runAfter, String error, Instant now) {
        jdbc.sql("""
                UPDATE jobs SET status = 'PENDING', locked_at = NULL, run_after = :runAfter,
                                last_error = :error, updated_at = :now
                 WHERE id = :id
                """)
                .param("runAfter", Timestamp.from(runAfter))
                .param("error", error)
                .param("now", Timestamp.from(now))
                .param("id", id)
                .update();
    }

    @Override
    public void markFailed(UUID id, String error, Instant now) {
        jdbc.sql("UPDATE jobs SET status = 'FAILED', locked_at = NULL, last_error = :error, updated_at = :now WHERE id = :id")
                .param("error", error)
                .param("now", Timestamp.from(now))
                .param("id", id)
                .update();
    }
}

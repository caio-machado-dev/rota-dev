package dev.rotadev.jobs.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.rotadev.jobs.Job;

/** O que o worker precisa do banco. Implementado em {@code infrastructure/JdbcJobStore}. */
public interface JobStore {

    /**
     * Marca até {@code limit} jobs prontos como RUNNING e os devolve. Inclui os RUNNING com
     * {@code locked_at} antes de {@code staleBefore}, de um worker que morreu no meio.
     */
    List<ClaimedJob> claim(Instant now, Instant staleBefore, int limit);

    void markSucceeded(UUID id, Instant now);

    void reschedule(UUID id, Instant runAfter, String error, Instant now);

    void markFailed(UUID id, String error, Instant now);

    /** Um job pego pelo worker, com o limite de tentativas dele. */
    record ClaimedJob(Job job, int maxAttempts) {
    }
}

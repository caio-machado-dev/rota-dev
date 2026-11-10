package dev.rotadev.sources.application;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.jobs.JobQueue;
import dev.rotadev.sources.domain.SourceType;

/** Caso de uso: o usuário pediu para reprocessar o GitHub agora. */
@Service
public class GithubSync {

    private final SourceRepository sources;
    private final JobQueue jobs;
    private final Clock clock;

    public GithubSync(SourceRepository sources, JobQueue jobs, Clock clock) {
        this.sources = sources;
        this.jobs = jobs;
        this.clock = clock;
    }

    /**
     * @return {@code false} se o usuário não tem GitHub conectado
     */
    @Transactional
    public boolean request(UUID userId) {
        if (!sources.exists(userId, SourceType.GITHUB)) {
            return false;
        }
        // A chave por minuto faz cliques repetidos virarem um job só (ADR 0006).
        long minute = clock.instant().truncatedTo(ChronoUnit.MINUTES).getEpochSecond() / 60;
        jobs.enqueue(userId, GithubIngestion.TYPE, "{}", GithubIngestion.TYPE + ":" + userId + ":manual:" + minute);
        return true;
    }
}

package dev.rotadev.jobs.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.rotadev.jobs.Job;
import dev.rotadev.jobs.JobHandler;
import dev.rotadev.jobs.domain.Backoff;

/**
 * Pega jobs prontos, entrega cada um ao handler do tipo e registra o resultado (ADR 0006).
 *
 * <p>Sem {@code @Transactional} de propósito: o {@code claim} é um único UPDATE atômico, e o handler
 * roda <b>fora</b> de qualquer transação do worker. Uma chamada lenta ao GitHub não pode segurar uma
 * conexão do banco aberta, e cada handler controla as próprias transações.
 */
@Service
public class JobWorker {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);

    /** Um RUNNING parado há mais que isso é de um worker que morreu: volta para a fila. */
    static final Duration STALE_AFTER = Duration.ofMinutes(15);

    private final JobStore store;
    private final Map<String, JobHandler> handlers;
    private final Backoff backoff = new Backoff(Duration.ofSeconds(30), Duration.ofHours(1));
    private final Clock clock;

    /**
     * O Spring injeta numa {@code List} todos os beans que implementam {@link JobHandler}, de
     * qualquer módulo. Montamos um mapa tipo → handler, e um tipo duplicado falha na subida.
     */
    public JobWorker(JobStore store, List<JobHandler> handlers, Clock clock) {
        this.store = store;
        this.handlers = handlers.stream().collect(Collectors.toMap(JobHandler::type, Function.identity()));
        this.clock = clock;
    }

    /** Processa um lote. Devolve quantos jobs pegou (o agendador chama de novo depois). */
    public int runOnce(int batchSize) {
        Instant now = clock.instant();
        List<JobStore.ClaimedJob> claimed = store.claim(now, now.minus(STALE_AFTER), batchSize);
        claimed.forEach(this::execute);
        return claimed.size();
    }

    private void execute(JobStore.ClaimedJob claimed) {
        Job job = claimed.job();
        JobHandler handler = handlers.get(job.type());
        if (handler == null) {
            store.markFailed(job.id(), "nenhum handler para o tipo " + job.type(), clock.instant());
            return;
        }
        try {
            handler.handle(job);
            store.markSucceeded(job.id(), clock.instant());
        } catch (Exception e) {
            String error = e.getClass().getSimpleName() + ": " + e.getMessage();
            Instant now = clock.instant();
            if (job.attempts() >= claimed.maxAttempts()) {
                log.warn("Job {} ({}) falhou de vez na tentativa {}: {}", job.id(), job.type(), job.attempts(), error);
                store.markFailed(job.id(), error, now);
            } else {
                store.reschedule(job.id(), now.plus(backoff.delayAfter(job.attempts())), error, now);
            }
        }
    }
}

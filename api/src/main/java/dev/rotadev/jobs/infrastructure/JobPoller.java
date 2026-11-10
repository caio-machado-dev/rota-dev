package dev.rotadev.jobs.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.rotadev.jobs.application.JobWorker;

/**
 * Relógio do worker: a cada {@code rota.jobs.poll-interval}, processa lotes até a fila esvaziar.
 *
 * <p>{@code @EnableScheduling} liga o agendador do Spring (sem ele, {@code @Scheduled} é ignorado).
 * {@code fixedDelay} espera o intervalo <i>depois</i> que a execução anterior termina, então duas
 * execuções nunca se sobrepõem no mesmo processo. Entre processos diferentes, quem protege é o
 * {@code SKIP LOCKED}.
 *
 * <p>Os testes desligam o poller ({@code rota.jobs.worker.enabled=false}) e chamam
 * {@link JobWorker#runOnce} na mão, para não haver uma thread de fundo mexendo na fila no meio do teste.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "rota.jobs.worker.enabled", havingValue = "true", matchIfMissing = true)
class JobPoller {

    private static final int BATCH_SIZE = 10;

    private final JobWorker worker;

    JobPoller(JobWorker worker) {
        this.worker = worker;
    }

    @Scheduled(fixedDelayString = "${rota.jobs.poll-interval:PT2S}")
    void poll() {
        while (worker.runOnce(BATCH_SIZE) == BATCH_SIZE) {
            // Lote cheio: provavelmente há mais jobs prontos, então segue sem esperar.
        }
    }
}

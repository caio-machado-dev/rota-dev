package dev.rotadev.jobs;

/**
 * Executa um tipo de job. Cada módulo registra os seus como beans ({@code @Component}), e o
 * worker escolhe pelo {@link #type()}.
 *
 * <p><b>Todo handler precisa ser idempotente</b> (ADR 0006): se a API cair depois do trabalho e
 * antes de marcar o job como concluído, ele roda de novo, e o resultado tem que ser o mesmo.
 */
public interface JobHandler {

    /** Nome do tipo, igual ao usado em {@link JobQueue#enqueue}. Ex.: {@code "github.ingest"}. */
    String type();

    /** Lançar uma exceção faz o job ser reagendado com backoff, até o limite de tentativas. */
    void handle(Job job) throws Exception;
}

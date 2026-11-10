package dev.rotadev.jobs;

import java.util.UUID;

/**
 * Enfileira trabalho lento ou que pode falhar para rodar fora da requisição HTTP (ADR 0006).
 *
 * <p>É a API pública do módulo {@code jobs}: o resto do código só conhece esta interface. Trocar o
 * Postgres por uma fila de verdade no futuro é reimplementar esta interface, sem tocar nos handlers.
 */
public interface JobQueue {

    /**
     * @param payloadJson    dados do job em JSON
     * @param idempotencyKey se já existir um job com esta chave, nada é criado
     */
    void enqueue(UUID userId, String type, String payloadJson, String idempotencyKey);
}

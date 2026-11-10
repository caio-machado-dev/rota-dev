package dev.rotadev.jobs;

import java.util.UUID;

/**
 * Um job que o worker entregou a um {@link JobHandler}.
 *
 * @param payload  JSON com os dados do job, como foi enfileirado
 * @param attempts tentativa atual, começando em 1
 */
public record Job(UUID id, UUID userId, String type, String payload, int attempts) {
}

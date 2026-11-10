package dev.rotadev.sources.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma fonte conectada, como o resto do código enxerga. <b>Sem o token</b>: quem precisa dele
 * (a ingestão do GitHub) pede explicitamente ao repositório. Assim ele não vaza por engano para
 * um log ou uma resposta HTTP.
 */
public record ConnectedSource(
        UUID id, UUID userId, SourceType type, String externalLogin, Instant connectedAt, Instant lastSyncedAt) {
}

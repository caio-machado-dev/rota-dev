package dev.rotadev.sources.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.rotadev.sources.domain.ConnectedSource;
import dev.rotadev.sources.domain.SourceType;

/** Persistência das fontes. Todo método recebe o {@code userId} (ADR 0004). */
public interface SourceRepository {

    /** Cria ou atualiza a fonte GitHub do usuário com um token novo. */
    void saveGithub(UUID userId, String githubLogin, String accessToken, String scopes, Instant now);

    List<ConnectedSource> findAllByUser(UUID userId);

    boolean exists(UUID userId, SourceType type);

    void markSynced(UUID userId, SourceType type, Instant now);

    /** O token em texto puro, já decifrado. Use só para chamar a API do GitHub. */
    Optional<String> findGithubToken(UUID userId);
}

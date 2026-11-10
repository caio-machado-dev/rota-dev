package dev.rotadev.sources.domain;

import java.time.Instant;
import java.util.Map;

/**
 * Um repositório do usuário no GitHub, com o que importa para o diagnóstico.
 *
 * @param languages bytes de código por linguagem, como a API do GitHub devolve
 * @param hasCi     se tem workflows do GitHub Actions
 */
public record GithubRepo(
        String name,
        String fullName,
        String htmlUrl,
        String description,
        boolean fork,
        boolean archived,
        int stars,
        Instant pushedAt,
        Map<String, Long> languages,
        boolean hasCi) {
}

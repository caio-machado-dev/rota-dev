package dev.rotadev.identity;

import java.util.Set;
import java.util.UUID;

/**
 * Evento: um usuário acabou de logar com o GitHub e temos um token de acesso novo.
 *
 * <p>Quem guarda o token é o módulo {@code sources}, não o {@code identity}. Só que o {@code sources}
 * já depende do {@code identity} (usa {@link AuthenticatedUser}); se o {@code identity} chamasse o
 * {@code sources}, haveria um ciclo, e o Spring Modulith barra ciclos (ADR 0002). Com um evento, o
 * {@code identity} só avisa, sem saber quem escuta. É o mesmo papel de um {@code signal} do Django.
 *
 * <p>O evento só existe na memória e é entregue na hora, na mesma thread. Ele nunca é gravado em
 * banco nem em log: por isso o {@link #toString()} esconde o token.
 */
public record GithubAccountLinked(UUID userId, String githubLogin, String accessToken, Set<String> scopes) {

    @Override
    public String toString() {
        return "GithubAccountLinked[userId=" + userId + ", githubLogin=" + githubLogin + ", accessToken=***, scopes=" + scopes + "]";
    }
}

package dev.rotadev.sources.application;

import java.time.Clock;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.identity.GithubAccountLinked;

/**
 * Guarda o token do GitHub quando o usuário loga (evento publicado pelo módulo {@code identity}).
 *
 * <p>{@code @EventListener} roda na mesma thread de quem publicou, logo depois do login. Usamos ele,
 * e não o {@code @ApplicationModuleListener} do Modulith, porque o evento carrega um token: o do
 * Modulith roda depois, em outra thread, e é pensado para eventos que podem ser gravados no banco
 * para reentrega. Token não pode ser gravado assim. O {@code @Transactional} dá a este método a
 * própria transação.
 */
@Service
public class GithubConnection {

    private final SourceRepository sources;
    private final Clock clock;

    public GithubConnection(SourceRepository sources, Clock clock) {
        this.sources = sources;
        this.clock = clock;
    }

    @EventListener
    @Transactional
    public void onGithubAccountLinked(GithubAccountLinked event) {
        sources.saveGithub(event.userId(), event.githubLogin(), event.accessToken(),
                String.join(",", event.scopes()), clock.instant());
    }
}

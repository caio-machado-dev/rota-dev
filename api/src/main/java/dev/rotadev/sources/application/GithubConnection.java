package dev.rotadev.sources.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.identity.GithubAccountLinked;
import dev.rotadev.jobs.JobQueue;

/**
 * Guarda o token do GitHub quando o usuário loga (evento publicado pelo módulo {@code identity})
 * e enfileira a ingestão dos repositórios.
 *
 * <p>{@code @EventListener} roda na mesma thread de quem publicou, logo depois do login. Usamos ele,
 * e não o {@code @ApplicationModuleListener} do Modulith, porque o evento carrega um token: o do
 * Modulith roda depois, em outra thread, e é pensado para eventos que podem ser gravados no banco
 * para reentrega. Token não pode ser gravado assim. O {@code @Transactional} dá a este método a
 * própria transação: fonte gravada e job enfileirado entram juntos, ou nenhum dos dois.
 */
@Service
public class GithubConnection {

    private final SourceRepository sources;
    private final JobQueue jobs;
    private final Clock clock;

    public GithubConnection(SourceRepository sources, JobQueue jobs, Clock clock) {
        this.sources = sources;
        this.jobs = jobs;
        this.clock = clock;
    }

    @EventListener
    @Transactional
    public void onGithubAccountLinked(GithubAccountLinked event) {
        Instant now = clock.instant();
        sources.saveGithub(event.userId(), event.githubLogin(), event.accessToken(),
                String.join(",", event.scopes()), now);
        // Cada login gera uma ingestão nova: a chave inclui o instante do login.
        jobs.enqueue(event.userId(), GithubIngestion.TYPE, "{}", GithubIngestion.TYPE + ":" + event.userId() + ":login:" + now.toEpochMilli());
    }
}

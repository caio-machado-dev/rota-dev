package dev.rotadev.sources.application;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Component;

import dev.rotadev.jobs.Job;
import dev.rotadev.jobs.JobHandler;
import dev.rotadev.profile.ProfileRecorder;
import dev.rotadev.sources.domain.GithubProfileAnalysis;
import dev.rotadev.sources.domain.GithubRepo;
import dev.rotadev.sources.domain.SourceType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Job {@value #TYPE}: busca os repositórios do usuário, analisa e grava um snapshot de perfil.
 *
 * <p>Idempotente (ADR 0006): o snapshot é gravado com o id do job, e o {@code ProfileRecorder}
 * ignora uma segunda gravação do mesmo job. Rodar de novo só repete as chamadas ao GitHub.
 */
@Component
public class GithubIngestion implements JobHandler {

    public static final String TYPE = "github.ingest";

    /** Os 30 repositórios mais recentes bastam para o diagnóstico e cabem no limite da API (5000/h). */
    static final int REPO_LIMIT = 30;

    private final SourceRepository sources;
    private final GithubApi github;
    private final ProfileRecorder profile;
    private final JsonMapper json;
    private final Clock clock;

    public GithubIngestion(SourceRepository sources, GithubApi github, ProfileRecorder profile, JsonMapper json, Clock clock) {
        this.sources = sources;
        this.github = github;
        this.profile = profile;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void handle(Job job) {
        String token = sources.findGithubToken(job.userId())
                .orElseThrow(() -> new IllegalStateException("usuário sem GitHub conectado"));

        List<GithubRepo> repos = github.recentOwnedRepos(token, REPO_LIMIT);
        GithubProfileAnalysis.Result result = GithubProfileAnalysis.analyze(repos);

        profile.record(job.userId(), SourceType.GITHUB.name(), job.id(), json.writeValueAsString(repos),
                result.skills(), result.evidences());
        sources.markSynced(job.userId(), SourceType.GITHUB, clock.instant());
    }
}

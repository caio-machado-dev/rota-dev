package dev.rotadev.sources.application;

import java.util.List;

import dev.rotadev.sources.domain.GithubRepo;

/** O que a ingestão precisa da API do GitHub. A implementação HTTP fica em {@code infrastructure/}. */
public interface GithubApi {

    /** Repositórios do dono do token, os mais recentes primeiro, já com linguagens e CI. */
    List<GithubRepo> recentOwnedRepos(String accessToken, int limit);
}

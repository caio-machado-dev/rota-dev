package dev.rotadev.sources.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import dev.rotadev.sources.application.GithubApi;
import dev.rotadev.sources.domain.GithubRepo;

/**
 * Cliente da API REST do GitHub com o {@link RestClient} do Spring.
 *
 * <p>O {@code RestClient.Builder} vem do Spring Boot já com os conversores JSON configurados. O
 * encadeamento {@code .get().uri(...).retrieve().body(...)} é o equivalente de
 * {@code requests.get(url).raise_for_status().json()}: status 4xx/5xx viram exceção.
 *
 * <p>Docs: https://docs.github.com/rest/repos/repos#list-repositories-for-the-authenticated-user
 */
@Component
class GithubRestApi implements GithubApi {

    private final RestClient http;

    GithubRestApi(RestClient.Builder builder, @Value("${rota.github.api-url:https://api.github.com}") String baseUrl) {
        this.http = builder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    @Override
    public List<GithubRepo> recentOwnedRepos(String accessToken, int limit) {
        List<RepoJson> repos = http.get()
                .uri("/user/repos?affiliation=owner&sort=pushed&per_page={limit}", limit)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });
        if (repos == null) {
            return List.of();
        }
        return repos.stream()
                .map(r -> new GithubRepo(r.name(), r.fullName(), r.htmlUrl(), r.description(), r.fork(), r.archived(),
                        r.stargazersCount(), r.pushedAt(), languages(accessToken, r.fullName()), hasWorkflows(accessToken, r.fullName())))
                .toList();
    }

    private Map<String, Long> languages(String token, String fullName) {
        Map<String, Long> languages = http.get()
                .uri("/repos/{owner}/{repo}/languages", pathOf(fullName))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });
        return languages == null ? Map.of() : languages;
    }

    private boolean hasWorkflows(String token, String fullName) {
        try {
            WorkflowsJson workflows = http.get()
                    .uri("/repos/{owner}/{repo}/actions/workflows", pathOf(fullName))
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(WorkflowsJson.class);
            return workflows != null && workflows.totalCount() > 0;
        } catch (HttpClientErrorException e) {
            // 404: Actions desabilitado no repositório. Não é erro, só não tem CI.
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw e;
        }
    }

    /**
     * "caio/rota-dev" vira {owner: caio, repo: rota-dev}. Passar o nome inteiro numa variável só
     * não funciona: o template codifica a barra como %2F, e o GitHub responde 404.
     */
    private static Map<String, String> pathOf(String fullName) {
        int slash = fullName.indexOf('/');
        return Map.of("owner", fullName.substring(0, slash), "repo", fullName.substring(slash + 1));
    }

    /**
     * Só os campos que usamos. {@code @JsonProperty} liga o nome em snake_case do JSON ao campo
     * do record; campos desconhecidos são ignorados pela configuração padrão do Boot.
     */
    record RepoJson(
            String name,
            @JsonProperty("full_name") String fullName,
            @JsonProperty("html_url") String htmlUrl,
            String description,
            boolean fork,
            boolean archived,
            @JsonProperty("stargazers_count") int stargazersCount,
            @JsonProperty("pushed_at") Instant pushedAt) {
    }

    record WorkflowsJson(@JsonProperty("total_count") int totalCount) {
    }
}

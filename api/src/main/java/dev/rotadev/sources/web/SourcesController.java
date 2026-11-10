package dev.rotadev.sources.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.rotadev.identity.AuthenticatedUser;
import dev.rotadev.sources.application.GithubSync;
import dev.rotadev.sources.application.SourceQueries;

@RestController
@RequestMapping("/api/sources")
class SourcesController {

    private final SourceQueries queries;
    private final GithubSync githubSync;

    SourcesController(SourceQueries queries, GithubSync githubSync) {
        this.queries = queries;
        this.githubSync = githubSync;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    List<SourceResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return queries.listFor(user.id()).stream()
                .map(s -> new SourceResponse(s.id(), s.type().name(), s.externalLogin(), s.connectedAt(), s.lastSyncedAt()))
                .toList();
    }

    /**
     * Pede uma nova leitura do GitHub. Responde 202 (Accepted): o trabalho foi aceito e vai rodar
     * num job, fora desta requisição (ADR 0006).
     */
    @PostMapping("/github/sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void syncGithub(@AuthenticationPrincipal AuthenticatedUser user) {
        if (!githubSync.request(user.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "GitHub não conectado");
        }
    }
}

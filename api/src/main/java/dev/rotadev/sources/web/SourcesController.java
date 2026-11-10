package dev.rotadev.sources.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.rotadev.identity.AuthenticatedUser;
import dev.rotadev.sources.application.SourceQueries;

@RestController
@RequestMapping("/api/sources")
class SourcesController {

    private final SourceQueries queries;

    SourcesController(SourceQueries queries) {
        this.queries = queries;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    List<SourceResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return queries.listFor(user.id()).stream()
                .map(s -> new SourceResponse(s.id(), s.type().name(), s.externalLogin(), s.connectedAt(), s.lastSyncedAt()))
                .toList();
    }
}

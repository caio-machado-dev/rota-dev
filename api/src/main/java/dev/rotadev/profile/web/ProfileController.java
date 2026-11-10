package dev.rotadev.profile.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.rotadev.identity.AuthenticatedUser;
import dev.rotadev.profile.ProfileReader;
import dev.rotadev.profile.web.ProfileResponses.ProfileResponse;
import dev.rotadev.profile.web.ProfileResponses.SnapshotResponse;

@RestController
@RequestMapping(path = "/api/profile", produces = MediaType.APPLICATION_JSON_VALUE)
class ProfileController {

    private final ProfileReader reader;

    ProfileController(ProfileReader reader) {
        this.reader = reader;
    }

    /** O perfil atual: o snapshot mais recente de cada fonte. */
    @GetMapping
    ProfileResponse current(@AuthenticationPrincipal AuthenticatedUser user) {
        return new ProfileResponse(reader.latestPerSource(user.id()).stream().map(SnapshotResponse::from).toList());
    }

    /**
     * {@code @PathVariable} lê o {@code {id}} da URL, como um parâmetro de rota do FastAPI. O id do
     * snapshot vem da URL, mas o do usuário vem da sessão: o snapshot de outra pessoa simplesmente
     * não é encontrado, e a resposta é 404, não 403 (ADR 0004).
     */
    @GetMapping("/snapshots/{id}")
    SnapshotResponse snapshot(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return reader.find(user.id(), id)
                .map(SnapshotResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

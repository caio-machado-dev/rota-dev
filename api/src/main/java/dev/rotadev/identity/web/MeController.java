package dev.rotadev.identity.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.rotadev.identity.AuthenticatedUser;
import dev.rotadev.identity.application.UserLookup;
import dev.rotadev.identity.domain.User;

/**
 * {@code @RestController} = cada método devolve o corpo da resposta, serializado em JSON
 * (como uma view do Django REST Framework). {@code @RequestMapping} define o prefixo das rotas.
 */
@RestController
@RequestMapping("/api/me")
class MeController {

    private final UserLookup lookup;

    MeController(UserLookup lookup) {
        this.lookup = lookup;
    }

    /**
     * {@code @AuthenticationPrincipal} injeta o usuário da sessão. O id vem sempre dali,
     * nunca da URL nem do corpo (ADR 0004).
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    MeResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        User user = lookup.byId(principal.id())
                // Sessão de um usuário que já foi apagado: trata como não logado.
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return new MeResponse(user.id(), user.login(), user.name(), user.email(), user.avatarUrl(), user.timeZone().getId());
    }
}

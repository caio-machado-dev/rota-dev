package dev.rotadev.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;

/**
 * Teste de unidade do caso de uso: sem Spring e sem banco. O repositório é um mock do
 * Mockito (como o {@code unittest.mock} do Python) e o relógio está parado em {@link #NOW}.
 */
class GithubSignInTest {

    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");

    private final UserRepository users = mock(UserRepository.class);
    private final GithubSignIn signIn = new GithubSignIn(users, Clock.fixed(NOW, ZoneOffset.UTC));

    private final GithubProfile profile = new GithubProfile(42L, "octocat", "Mona", null, null);

    @Test
    void firstSignInCreatesUser() {
        User created = userWithId(UUID.randomUUID());
        when(users.findByGithubId(42L)).thenReturn(Optional.empty());
        when(users.create(profile, NOW)).thenReturn(created);

        assertThat(signIn.signIn(profile)).isEqualTo(created);
        verify(users, never()).updateProfile(any(), any(), any());
    }

    // TODO(caio): escrever o caso do segundo login e remover o @Disabled.
    //
    // O que fazer:
    //   1. Faça o mock devolver um usuário existente em findByGithubId(42L).
    //   2. Faça updateProfile(id do existente, profile, NOW) devolver um usuário atualizado.
    //   3. Confira que signIn devolve o atualizado e que create nunca foi chamado
    //      (use verify(users, never()).create(any(), any()), como no teste acima).
    //
    // Critério de pronto: `./mvnw test -Dtest=GithubSignInTest` passa com 2 testes, nenhum pulado.
    @Disabled("TODO(caio): exercício da Fase 1")
    @Test
    void secondSignInUpdatesExistingUserInsteadOfCreating() {
    }

    private static User userWithId(UUID id) {
        return new User(id, 42L, "octocat", "Mona", null, null, User.DEFAULT_TIME_ZONE, NOW, NOW);
    }
}

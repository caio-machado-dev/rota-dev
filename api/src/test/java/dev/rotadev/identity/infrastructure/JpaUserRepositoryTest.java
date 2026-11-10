package dev.rotadev.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.shared.SecretCipher;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;

/**
 * Testa o repositório contra um Postgres real, com as migrações do Flyway aplicadas.
 *
 * <p>{@code @DataJpaTest} sobe só a fatia de JPA do Spring (sem web, sem os outros beans),
 * por isso o {@link JpaUserRepository} entra pelo {@code @Import}. Cada teste roda numa
 * transação que sofre rollback no fim, então um teste não enxerga os dados do outro.
 */
@DataJpaTest
// SecretCipher: a fatia de JPA carrega todas as entidades do projeto, e a de connected_sources
// usa um conversor que cifra o token (ADR 0008). Sem a cifra no contexto, o Hibernate não sobe.
@Import({TestcontainersConfiguration.class, JpaUserRepository.class, SecretCipher.class})
class JpaUserRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-11-10T12:00:00Z");
    private static final Instant T1 = Instant.parse("2026-11-11T12:00:00Z");

    @Autowired
    JpaUserRepository users;

    @Test
    void createsUserWithUuidV7AndDefaultTimeZone() {
        User created = users.create(new GithubProfile(42L, "octocat", "Mona", null, null), T0);

        assertThat(created.id()).isNotNull();
        assertThat(created.id().version()).isEqualTo(7);
        assertThat(created.timeZone()).isEqualTo(User.DEFAULT_TIME_ZONE);
        assertThat(created.createdAt()).isEqualTo(T0);
        assertThat(users.findByGithubId(42L)).contains(created);
    }

    @Test
    void updatesProfileKeepingIdAndCreatedAt() {
        User created = users.create(new GithubProfile(42L, "octocat", "Mona", null, null), T0);

        User updated = users.updateProfile(
                created.id(), new GithubProfile(42L, "octocat2", "Mona Lisa", "mona@example.com", null), T1);

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.login()).isEqualTo("octocat2");
        assertThat(updated.email()).isEqualTo("mona@example.com");
        assertThat(updated.createdAt()).isEqualTo(T0);
        assertThat(updated.updatedAt()).isEqualTo(T1);
    }

    @Test
    void returnsEmptyForUnknownGithubId() {
        assertThat(users.findByGithubId(999L)).isEmpty();
    }
}

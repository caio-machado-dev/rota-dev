package dev.rotadev.identity.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;

/**
 * O que os casos de uso precisam do banco, sem saber que é JPA.
 *
 * <p>A camada {@code application} só pode depender de {@code domain} (ADR 0002). Por isso a
 * interface mora aqui e a implementação ({@code JpaUserRepository}) mora em
 * {@code infrastructure/}: quem depende de quem é a infraestrutura, e não o contrário.
 * Nos testes do caso de uso, basta um mock desta interface, sem banco.
 */
public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByGithubId(long githubId);

    /** Cria um usuário novo com id UUID v7 e o fuso padrão. */
    User create(GithubProfile profile, Instant now);

    /** Atualiza login, nome, e-mail e avatar de um usuário que já existe. */
    User updateProfile(UUID id, GithubProfile profile, Instant now);
}

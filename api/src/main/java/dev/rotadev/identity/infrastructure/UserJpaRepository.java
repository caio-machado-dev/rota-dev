package dev.rotadev.identity.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório do Spring Data. Ninguém implementa esta interface: o Spring gera a classe na
 * subida, e o SQL de {@code findByGithubId} sai do próprio nome do método
 * ({@code where github_id = ?}). É o equivalente de {@code User.objects.filter(github_id=...)}.
 */
interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByGithubId(long githubId);
}

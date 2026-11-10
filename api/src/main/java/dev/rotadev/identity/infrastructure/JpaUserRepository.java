package dev.rotadev.identity.infrastructure;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import dev.rotadev.identity.application.UserRepository;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.identity.domain.User;

/**
 * Implementa o {@link UserRepository} da aplicação com JPA e faz a conversão
 * entidade ↔ record de domínio, escrita à mão (ADR 0003).
 */
@Repository
class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpa;

    JpaUserRepository(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByGithubId(long githubId) {
        return jpa.findByGithubId(githubId).map(JpaUserRepository::toDomain);
    }

    @Override
    public User create(GithubProfile profile, Instant now) {
        var entity = new UserEntity(profile.githubId(), profile.login(), User.DEFAULT_TIME_ZONE.getId(), now);
        entity.updateProfile(profile.login(), profile.name(), profile.email(), profile.avatarUrl(), now);
        return toDomain(jpa.save(entity));
    }

    @Override
    public User updateProfile(UUID id, GithubProfile profile, Instant now) {
        var entity = jpa.findById(id).orElseThrow(() -> new IllegalStateException("usuário " + id + " não existe"));
        entity.updateProfile(profile.login(), profile.name(), profile.email(), profile.avatarUrl(), now);
        return toDomain(jpa.save(entity));
    }

    private static User toDomain(UserEntity e) {
        return new User(
                e.getId(),
                e.getGithubId(),
                e.getLogin(),
                e.getName(),
                e.getEmail(),
                e.getAvatarUrl(),
                ZoneId.of(e.getTimeZone()),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}

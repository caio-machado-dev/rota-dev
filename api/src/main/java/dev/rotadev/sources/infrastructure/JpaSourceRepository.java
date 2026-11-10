package dev.rotadev.sources.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import dev.rotadev.sources.application.SourceRepository;
import dev.rotadev.sources.domain.ConnectedSource;
import dev.rotadev.sources.domain.SourceType;

@Repository
class JpaSourceRepository implements SourceRepository {

    private final ConnectedSourceJpaRepository jpa;

    JpaSourceRepository(ConnectedSourceJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void saveGithub(UUID userId, String githubLogin, String accessToken, String scopes, Instant now) {
        var entity = jpa.findByUserIdAndType(userId, SourceType.GITHUB)
                .orElseGet(() -> new ConnectedSourceEntity(userId, SourceType.GITHUB, now));
        entity.updateCredentials(githubLogin, accessToken, scopes, now);
        jpa.save(entity);
    }

    @Override
    public List<ConnectedSource> findAllByUser(UUID userId) {
        return jpa.findAllByUserIdOrderByConnectedAt(userId).stream().map(JpaSourceRepository::toDomain).toList();
    }

    @Override
    public Optional<String> findGithubToken(UUID userId) {
        return jpa.findByUserIdAndType(userId, SourceType.GITHUB).map(ConnectedSourceEntity::getAccessToken);
    }

    private static ConnectedSource toDomain(ConnectedSourceEntity e) {
        return new ConnectedSource(e.getId(), e.getUserId(), e.getType(), e.getExternalLogin(), e.getConnectedAt(), e.getLastSyncedAt());
    }
}

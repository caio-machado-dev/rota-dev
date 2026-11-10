package dev.rotadev.sources.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.rotadev.sources.domain.SourceType;

interface ConnectedSourceJpaRepository extends JpaRepository<ConnectedSourceEntity, UUID> {

    Optional<ConnectedSourceEntity> findByUserIdAndType(UUID userId, SourceType type);

    List<ConnectedSourceEntity> findAllByUserIdOrderByConnectedAt(UUID userId);
}

package dev.rotadev.sources.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.rotadev.sources.domain.SourceFile;

public interface SourceFileRepository {

    void insert(SourceFile file);

    Optional<SourceFile> findBySha256(UUID userId, String sha256);

    List<SourceFile> findAllByUser(UUID userId);
}

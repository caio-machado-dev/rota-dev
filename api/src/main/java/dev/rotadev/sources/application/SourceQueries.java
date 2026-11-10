package dev.rotadev.sources.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.sources.domain.ConnectedSource;

@Service
public class SourceQueries {

    private final SourceRepository sources;

    public SourceQueries(SourceRepository sources) {
        this.sources = sources;
    }

    @Transactional(readOnly = true)
    public List<ConnectedSource> listFor(UUID userId) {
        return sources.findAllByUser(userId);
    }
}

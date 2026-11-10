package dev.rotadev.profile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** API pública de leitura: o diagnóstico (Fase 2) lê o perfil por aqui. */
public interface ProfileReader {

    /** O snapshot mais recente de cada fonte do usuário. */
    List<Snapshot> latestPerSource(UUID userId);

    /** Um snapshot do usuário. Vazio se não existir <b>ou se for de outro usuário</b> (ADR 0004). */
    Optional<Snapshot> find(UUID userId, UUID snapshotId);
}

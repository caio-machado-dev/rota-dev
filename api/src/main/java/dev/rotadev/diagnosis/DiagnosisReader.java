package dev.rotadev.diagnosis;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública do módulo {@code diagnosis}: o plano lê o diagnóstico por aqui. Os records abaixo
 * repetem só o que outro módulo precisa; os tipos internos ({@code domain/}) continuam livres para mudar.
 */
public interface DiagnosisReader {

    /** O diagnóstico pronto mais recente do usuário. */
    Optional<ReadyDiagnosis> latestReady(UUID userId);

    record ReadyDiagnosis(UUID id, String estimatedLevel, List<AxisScore> axes, List<GapItem> gaps) {
    }

    record AxisScore(String key, String name, int required, int score, boolean verified) {
    }

    record GapItem(String axisKey, String axisName, int size, int priority, String description) {
    }
}

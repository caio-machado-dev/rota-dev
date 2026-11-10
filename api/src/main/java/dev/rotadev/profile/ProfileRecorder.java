package dev.rotadev.profile;

import java.util.List;
import java.util.UUID;

/**
 * API pública do módulo {@code profile} para quem produz dados (a ingestão de cada fonte).
 * As fontes dependem deste módulo, e não o contrário.
 */
public interface ProfileRecorder {

    /**
     * Grava um snapshot novo com as evidências dele.
     *
     * @param jobId   job que produziu o snapshot; se já houver um snapshot desse job, nada é gravado
     * @param rawJson resposta bruta da fonte, em JSON
     * @return {@code false} se o job já tinha gravado (o job rodou de novo)
     */
    boolean record(UUID userId, String sourceType, UUID jobId, String rawJson, List<SkillSignal> skills, List<Evidence> evidences);
}

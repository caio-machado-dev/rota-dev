package dev.rotadev.plan;

import java.util.UUID;

/** API pública de escrita do plano: os reajustes pedem uma versão nova por aqui. */
public interface PlanWriter {

    /**
     * Enfileira uma versão nova do plano.
     *
     * @param reason   motivo que fica no histórico ("Reajuste aceito no check-in de 16/11: ...")
     * @param guidance orientação extra para a IA montar esta versão
     * @return id da versão criada (PENDING)
     */
    UUID requestVersion(UUID userId, String reason, String guidance);
}

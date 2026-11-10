package dev.rotadev.ai;

import java.util.UUID;

/**
 * Limite de gerações de IA por mês, conforme o nível do usuário. Quem enfileira uma geração
 * (diagnóstico, plano) confere aqui antes: barrar depois de chamar a IA já seria custo gasto.
 */
public interface AiQuota {

    /** @throws AiQuotaExceededException se o usuário já usou todas as gerações do mês */
    void requireAvailable(UUID userId);
}

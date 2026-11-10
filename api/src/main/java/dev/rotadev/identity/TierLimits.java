package dev.rotadev.identity;

/** API pública: limites de uso de cada nível ({@code rota.limits.*}), aplicados por cada módulo. */
public interface TierLimits {

    /** Gerações de IA que deram certo, por mês do calendário (UTC). */
    int aiGenerationsPerMonth(Tier tier);

    /** Arquivos guardados ao mesmo tempo. */
    int uploads(Tier tier);
}

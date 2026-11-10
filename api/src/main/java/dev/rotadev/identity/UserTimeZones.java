package dev.rotadev.identity;

import java.time.ZoneId;
import java.util.UUID;

/**
 * Fuso de cada usuário, para calcular o "hoje" dele (ADR 0009). API pública do módulo: quem precisa
 * de "hoje" (prazo do objetivo, painel Agora, check-in de domingo) pergunta por aqui.
 */
public interface UserTimeZones {

    ZoneId of(UUID userId);
}

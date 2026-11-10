package dev.rotadev.identity;

import java.util.UUID;

/** API pública: em que nível o usuário está, para os módulos aplicarem os limites. */
public interface UserTiers {

    Tier tierOf(UUID userId);
}

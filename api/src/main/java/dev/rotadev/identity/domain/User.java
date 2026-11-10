package dev.rotadev.identity.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Um usuário do Rota Dev, como o resto do código enxerga.
 *
 * <p>Fica em {@code domain/} e por isso não conhece JPA nem Spring (ADR 0002). A versão que vai
 * para o banco é a {@code UserEntity}, em {@code infrastructure/}; as duas se convertem à mão.
 */
public record User(
        UUID id,
        long githubId,
        String login,
        String name,
        String email,
        String avatarUrl,
        ZoneId timeZone,
        Instant createdAt,
        Instant updatedAt) {

    /** Fuso de quem ainda não escolheu um (ADR 0009). Igual ao DEFAULT da coluna em V2__users.sql. */
    public static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("America/Sao_Paulo");
}

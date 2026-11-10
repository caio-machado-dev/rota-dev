package dev.rotadev.identity.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Corpo do {@code GET /api/me}. Record da camada web: o contrato da API não depende da entidade (ADR 0003).
 *
 * <p>{@code @NotNull} aqui não valida nada em tempo de execução: o springdoc lê a anotação e marca o
 * campo como obrigatório no OpenAPI. Assim o tipo gerado no front é {@code login: string}, e não
 * {@code login?: string}. Os campos sem anotação podem vir {@code null}.
 */
public record MeResponse(
        @NotNull UUID id,
        @NotNull String login,
        String name,
        String email,
        String avatarUrl,
        @NotNull String timeZone) {
}

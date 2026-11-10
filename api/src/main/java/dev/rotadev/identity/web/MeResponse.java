package dev.rotadev.identity.web;

import java.util.UUID;

/** Corpo do {@code GET /api/me}. Record da camada web: o contrato da API não depende da entidade (ADR 0003). */
public record MeResponse(UUID id, String login, String name, String email, String avatarUrl, String timeZone) {
}

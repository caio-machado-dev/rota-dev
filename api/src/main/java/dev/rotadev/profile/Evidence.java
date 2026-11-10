package dev.rotadev.profile;

import java.util.UUID;

/**
 * Fato verificável que sustenta uma conclusão (glossário: "Evidência").
 *
 * @param id    nulo enquanto ainda não foi gravada
 * @param kind  tipo de fato, ex.: {@code REPOSITORY}, {@code EXPERIENCE}
 * @param skill habilidade que a evidência sustenta, ou {@code null}
 * @param url   onde conferir, quando houver (ex.: o repositório no GitHub)
 */
public record Evidence(UUID id, String kind, String skill, String title, String detail, String url, Confidence confidence) {
}

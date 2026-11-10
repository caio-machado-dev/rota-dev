package dev.rotadev.sources.domain;

import java.util.List;

/**
 * O que aproveitamos do export de dados do LinkedIn (Configurações → Privacidade dos dados →
 * Obter uma cópia dos seus dados). O ZIP traz um CSV por assunto; usamos três.
 */
public record LinkedInData(List<Position> positions, List<String> skills, List<Certification> certifications) {

    /** Uma linha de Positions.csv. Datas como o LinkedIn exporta ("Mar 2024"); {@code finishedOn} vazio = atual. */
    public record Position(String company, String title, String description, String startedOn, String finishedOn) {
    }

    /** Uma linha de Certifications.csv. */
    public record Certification(String name, String authority, String startedOn) {
    }
}

package dev.rotadev.profile;

/**
 * Uma habilidade que a fonte sugere.
 *
 * @param weight peso relativo de 0 a 1 dentro do snapshot (ex.: fatia do código do usuário em Java)
 */
public record SkillSignal(String name, double weight, Confidence confidence) {

    public SkillSignal {
        if (weight < 0 || weight > 1) {
            throw new IllegalArgumentException("weight deve estar entre 0 e 1");
        }
    }
}

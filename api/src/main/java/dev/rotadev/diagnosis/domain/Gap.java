package dev.rotadev.diagnosis.domain;

/** Lacuna (glossário): distância entre a nota atual e a exigida num eixo. Prioridade 1 = mais urgente. */
public record Gap(String axisKey, String axisName, int size, int priority, String description) {
}

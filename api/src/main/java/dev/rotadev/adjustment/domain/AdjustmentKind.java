package dev.rotadev.adjustment.domain;

public enum AdjustmentKind {
    /** Semana difícil (pouca energia e um bloqueio): aliviar a próxima. */
    LIGHTER_WEEK,
    /** Duas semanas bem abaixo do orçamento: o plano está pesado demais para a vida real. */
    REDUCE_LOAD,
    /** Duas semanas bem acima: há folga para antecipar. */
    INCREASE_LOAD,
    /** Poucas revisões de código gerado por IA na semana. */
    MORE_REVIEW
}

package dev.rotadev.adjustment.application;

/** A proposta não existe, é de outro usuário ou já foi decidida. Vira 404. */
public class AdjustmentNotFoundException extends RuntimeException {

    public AdjustmentNotFoundException() {
        super("reajuste não encontrado");
    }
}

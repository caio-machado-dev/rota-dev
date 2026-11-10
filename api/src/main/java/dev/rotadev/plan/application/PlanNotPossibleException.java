package dev.rotadev.plan.application;

/** Falta algo para montar o plano (objetivo ou diagnóstico pronto). A mensagem vai para o usuário. */
public class PlanNotPossibleException extends RuntimeException {

    public PlanNotPossibleException(String message) {
        super(message);
    }
}

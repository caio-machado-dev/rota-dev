package dev.rotadev.diagnosis.application;

/** Faltam dados para diagnosticar (sem objetivo ou sem nenhuma fonte lida). A mensagem vai para o usuário. */
public class DiagnosisNotPossibleException extends RuntimeException {

    public DiagnosisNotPossibleException(String message) {
        super(message);
    }
}

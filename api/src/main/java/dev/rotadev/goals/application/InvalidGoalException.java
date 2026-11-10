package dev.rotadev.goals.application;

/** Objetivo recusado por uma regra de negócio. A mensagem vai para o usuário. */
public class InvalidGoalException extends RuntimeException {

    public InvalidGoalException(String message) {
        super(message);
    }
}

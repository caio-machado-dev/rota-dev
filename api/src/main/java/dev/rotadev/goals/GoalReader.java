package dev.rotadev.goals;

import java.util.Optional;
import java.util.UUID;

/** API pública: o diagnóstico e o plano leem o objetivo por aqui. */
public interface GoalReader {

    Optional<CareerGoal> find(UUID userId);
}

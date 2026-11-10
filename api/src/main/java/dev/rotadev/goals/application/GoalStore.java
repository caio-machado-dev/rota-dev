package dev.rotadev.goals.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import dev.rotadev.goals.CareerGoal;

public interface GoalStore {

    void upsert(CareerGoal goal, Instant now);

    Optional<CareerGoal> find(UUID userId);
}

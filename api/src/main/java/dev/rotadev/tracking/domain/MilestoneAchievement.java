package dev.rotadev.tracking.domain;

import java.time.LocalDate;
import java.util.UUID;

/** Portão alcançado, com o link que prova (um PR, um deploy, um repositório). */
public record MilestoneAchievement(UUID id, int planVersion, String milestone, String evidenceUrl, LocalDate achievedOn) {
}

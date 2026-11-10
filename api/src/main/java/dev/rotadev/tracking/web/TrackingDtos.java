package dev.rotadev.tracking.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import dev.rotadev.tracking.domain.ActivityKind;
import dev.rotadev.tracking.domain.ReviewHabit;
import dev.rotadev.tracking.domain.StudySession;
import dev.rotadev.tracking.domain.TemplateSlot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Records de entrada e saída do acompanhamento. Os nomes levam o prefixo do assunto porque o springdoc
 * usa o nome simples da classe no OpenAPI, e dois {@code Slot} de módulos diferentes colidiriam.
 */
final class TrackingDtos {

    private TrackingDtos() {
    }

    /** @param day vazio = hoje, no fuso do usuário */
    record SessionRequest(
            LocalDate day,
            @NotNull ActivityKind kind,
            @Min(5) @Max(600) int minutes,
            @NotBlank @Size(max = 200) String topic,
            @Size(max = 500) String learning) {
    }

    record SessionResponse(@NotNull UUID id, @NotNull LocalDate day, @NotNull String kind, @NotNull int minutes,
            @NotNull String topic, String learning) {

        static SessionResponse from(StudySession s) {
            return new SessionResponse(s.id(), s.day(), s.kind().name(), s.minutes(), s.topic(), s.learning());
        }
    }

    /** @param weekday 1 = segunda ... 7 = domingo */
    record TemplateSlotDto(@Min(1) @Max(7) int weekday, @NotNull ActivityKind kind, @Min(5) @Max(600) int minutes) {

        static TemplateSlotDto from(TemplateSlot s) {
            return new TemplateSlotDto(s.weekday().getValue(), s.kind(), s.minutes());
        }
    }

    record TemplateRequest(@NotNull @Size(max = 28) List<@Valid TemplateSlotDto> slots) {
    }

    record HabitRequest(@NotNull LocalDate day, boolean readDiff, boolean explained, boolean tested, @Size(max = 1000) String note) {
    }

    record HabitResponse(@NotNull LocalDate day, @NotNull boolean readDiff, @NotNull boolean explained, @NotNull boolean tested,
            String note, @NotNull int stepsDone, @NotNull boolean complete) {

        static HabitResponse from(ReviewHabit h) {
            return new HabitResponse(h.day(), h.readDiff(), h.explained(), h.tested(), h.note(), h.stepsDone(), h.complete());
        }
    }

    record NowTask(@NotNull String title, @NotNull String kind, @NotNull int minutes) {
    }

    record NowPlanWeek(@NotNull int planVersion, @NotNull int index, @NotNull String theme, @NotNull List<NowTask> tasks,
            @NotNull int totalMinutes) {
    }

    record NowResponse(
            @NotNull LocalDate today,
            @NotNull int weekday,
            NowPlanWeek planWeek,
            @NotNull List<TemplateSlotDto> templateToday,
            @NotNull List<SessionResponse> sessionsToday,
            @NotNull int minutesToday,
            @NotNull int minutesThisWeek,
            Integer weeklyBudgetMinutes,
            HabitResponse habitToday) {
    }
}

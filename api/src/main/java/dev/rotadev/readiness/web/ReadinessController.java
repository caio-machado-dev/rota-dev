package dev.rotadev.readiness.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.rotadev.identity.AuthenticatedUser;
import dev.rotadev.readiness.application.ReadinessService;
import dev.rotadev.readiness.domain.ReadinessScore;
import jakarta.validation.constraints.NotNull;

@RestController
class ReadinessController {

    private final ReadinessService readiness;

    ReadinessController(ReadinessService readiness) {
        this.readiness = readiness;
    }

    record ReadinessBreakdown(@NotNull int score, @NotNull int skills, @NotNull int gates, @NotNull int study, @NotNull int habit) {

        static ReadinessBreakdown from(ReadinessScore s) {
            return new ReadinessBreakdown(s.score(), s.skills(), s.gates(), s.study(), s.habit());
        }
    }

    record ReadinessPoint(@NotNull LocalDate weekStart, @NotNull ReadinessBreakdown value) {
    }

    record ReadinessResponse(@NotNull ReadinessBreakdown current, @NotNull List<ReadinessPoint> history) {
    }

    /** O índice agora e um ponto por check-in, do mais antigo ao mais recente. */
    @GetMapping(path = "/api/readiness", produces = MediaType.APPLICATION_JSON_VALUE)
    ReadinessResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
        return new ReadinessResponse(ReadinessBreakdown.from(readiness.current(user.id())),
                readiness.history(user.id()).stream().map(p -> new ReadinessPoint(p.weekStart(), ReadinessBreakdown.from(p.score()))).toList());
    }
}

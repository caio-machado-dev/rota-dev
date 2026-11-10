package dev.rotadev.adjustment.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.rotadev.adjustment.application.AdjustmentNotFoundException;
import dev.rotadev.adjustment.application.AdjustmentService;
import dev.rotadev.adjustment.application.AdjustmentStore.StoredAdjustment;
import dev.rotadev.identity.AuthenticatedUser;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping(path = "/api/adjustments", produces = MediaType.APPLICATION_JSON_VALUE)
class AdjustmentController {

    private final AdjustmentService adjustments;

    AdjustmentController(AdjustmentService adjustments) {
        this.adjustments = adjustments;
    }

    record AdjustmentResponse(@NotNull UUID id, @NotNull LocalDate weekStart, @NotNull String kind, @NotNull String reason,
            @NotNull String status, UUID planVersionId, @NotNull Instant createdAt) {

        static AdjustmentResponse from(StoredAdjustment a) {
            return new AdjustmentResponse(a.id(), a.weekStart(), a.proposal().kind().name(), a.proposal().reason(), a.status(),
                    a.planVersionId(), a.createdAt());
        }
    }

    record AdjustmentAccepted(@NotNull UUID planVersionId) {
    }

    @GetMapping
    List<AdjustmentResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return adjustments.list(user.id()).stream().map(AdjustmentResponse::from).toList();
    }

    /** 202: a versão nova do plano é montada num job; o front acompanha em /api/plan/latest. */
    @PostMapping("/{id}/accept")
    @ResponseStatus(HttpStatus.ACCEPTED)
    AdjustmentAccepted accept(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return new AdjustmentAccepted(adjustments.accept(user.id(), id));
    }

    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        adjustments.reject(user.id(), id);
    }

    @ExceptionHandler(AdjustmentNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ProblemDetail.forStatus(HttpStatus.NOT_FOUND));
    }
}

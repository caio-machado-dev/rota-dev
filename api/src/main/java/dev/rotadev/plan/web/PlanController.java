package dev.rotadev.plan.web;

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
import org.springframework.web.server.ResponseStatusException;

import dev.rotadev.identity.AuthenticatedUser;
import dev.rotadev.plan.application.PlanNotPossibleException;
import dev.rotadev.plan.application.PlanStore;
import dev.rotadev.plan.application.RequestPlan;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping(path = "/api/plan", produces = MediaType.APPLICATION_JSON_VALUE)
class PlanController {

    private final RequestPlan request;
    private final PlanStore plans;

    PlanController(RequestPlan request, PlanStore plans) {
        this.request = request;
        this.plans = plans;
    }

    record Created(@NotNull UUID id, @NotNull int version) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    Created create(@AuthenticationPrincipal AuthenticatedUser user) {
        var created = request.request(user.id(), null);
        return new Created(created.id(), created.version());
    }

    /** A versão mais recente, em qualquer status: o front mostra "gerando…" enquanto PENDING. */
    @GetMapping("/latest")
    PlanResponse latest(@AuthenticationPrincipal AuthenticatedUser user) {
        return plans.latest(user.id()).map(PlanResponse::from).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** O plano em vigor: a versão pronta mais recente. */
    @GetMapping("/current")
    PlanResponse current(@AuthenticationPrincipal AuthenticatedUser user) {
        return plans.current(user.id()).map(PlanResponse::from).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/versions")
    List<PlanResponse> history(@AuthenticationPrincipal AuthenticatedUser user) {
        return plans.history(user.id()).stream().map(PlanResponse::from).toList();
    }

    @GetMapping("/versions/{id}")
    PlanResponse version(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return plans.find(user.id(), id).map(PlanResponse::from).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(PlanNotPossibleException.class)
    ResponseEntity<ProblemDetail> notPossible(PlanNotPossibleException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage()));
    }
}

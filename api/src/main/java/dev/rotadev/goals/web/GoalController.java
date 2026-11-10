package dev.rotadev.goals.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.rotadev.goals.CareerGoal;
import dev.rotadev.goals.application.GoalService;
import dev.rotadev.goals.application.InvalidGoalException;
import dev.rotadev.identity.AuthenticatedUser;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "/api/goal", produces = MediaType.APPLICATION_JSON_VALUE)
class GoalController {

    private final GoalService goals;

    GoalController(GoalService goals) {
        this.goals = goals;
    }

    /** 404 enquanto o usuário não definiu um objetivo: o front mostra o formulário vazio. */
    @GetMapping
    GoalResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
        return goals.find(user.id()).map(GoalResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** PUT porque o objetivo é um recurso único do usuário: enviar de novo substitui o anterior. */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    GoalResponse save(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody GoalRequest body) {
        CareerGoal goal = new CareerGoal(user.id(), body.targetRole().trim(), body.targetLevel(), body.track(), body.deadline(),
                body.hoursPerWeek(), blankToNull(body.primaryStack()), body.wantsCertifications(), blankToNull(body.notes()));
        return GoalResponse.from(goals.save(goal));
    }

    @ExceptionHandler({InvalidGoalException.class, IllegalArgumentException.class})
    ResponseEntity<ProblemDetail> invalid(RuntimeException e) {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

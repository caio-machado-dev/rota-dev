package dev.rotadev.tracking.web;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.rotadev.identity.AuthenticatedUser;
import dev.rotadev.tracking.application.InvalidTrackingException;
import dev.rotadev.tracking.application.NowView;
import dev.rotadev.tracking.application.TrackingService;
import dev.rotadev.tracking.domain.ReviewHabit;
import dev.rotadev.tracking.domain.StudySession;
import dev.rotadev.tracking.domain.TemplateSlot;
import dev.rotadev.tracking.web.TrackingDtos.HabitRequest;
import dev.rotadev.tracking.web.TrackingDtos.HabitResponse;
import dev.rotadev.tracking.web.TrackingDtos.NowPlanWeek;
import dev.rotadev.tracking.web.TrackingDtos.NowResponse;
import dev.rotadev.tracking.web.TrackingDtos.NowTask;
import dev.rotadev.tracking.web.TrackingDtos.SessionRequest;
import dev.rotadev.tracking.web.TrackingDtos.SessionResponse;
import dev.rotadev.tracking.web.TrackingDtos.TemplateRequest;
import dev.rotadev.tracking.web.TrackingDtos.TemplateSlotDto;
import jakarta.validation.Valid;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
class TrackingController {

    private final TrackingService tracking;

    TrackingController(TrackingService tracking) {
        this.tracking = tracking;
    }

    /** Painel "Agora": tudo o que importa hoje, no fuso do usuário. */
    @GetMapping("/api/now")
    NowResponse now(@AuthenticationPrincipal AuthenticatedUser user) {
        NowView v = tracking.now(user.id());
        NowPlanWeek week = v.planWeek() == null ? null : new NowPlanWeek(v.planWeek().planVersion(), v.planWeek().index(),
                v.planWeek().theme(), v.planWeek().tasks().stream().map(t -> new NowTask(t.title(), t.kind(), t.minutes())).toList(),
                v.planWeek().totalMinutes());
        return new NowResponse(v.today(), v.weekday().getValue(), week,
                v.templateToday().stream().map(TemplateSlotDto::from).toList(),
                v.sessionsToday().stream().map(SessionResponse::from).toList(),
                v.minutesToday(), v.minutesThisWeek(), v.weeklyBudgetMinutes(),
                v.habitToday() == null ? null : HabitResponse.from(v.habitToday()));
    }

    @PostMapping(path = "/api/tracking/sessions", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    SessionResponse logSession(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody SessionRequest body) {
        return SessionResponse.from(tracking.logSession(user.id(),
                new StudySession(null, body.day(), body.kind(), body.minutes(), body.topic(), body.learning())));
    }

    /**
     * {@code @DateTimeFormat(iso = DATE)} lê "2026-11-10" da query string para um {@link LocalDate},
     * como um parâmetro tipado {@code date} do FastAPI.
     */
    @GetMapping("/api/tracking/sessions")
    List<SessionResponse> sessions(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to.isBefore(from) || from.plusDays(366).isBefore(to)) {
            throw new InvalidTrackingException("Período inválido (até um ano).");
        }
        return tracking.sessions(user.id(), from, to).stream().map(SessionResponse::from).toList();
    }

    @DeleteMapping("/api/tracking/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteSession(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        if (!tracking.deleteSession(user.id(), id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/api/tracking/template")
    List<TemplateSlotDto> template(@AuthenticationPrincipal AuthenticatedUser user) {
        return tracking.template(user.id()).stream().map(TemplateSlotDto::from).toList();
    }

    @PutMapping(path = "/api/tracking/template", consumes = MediaType.APPLICATION_JSON_VALUE)
    List<TemplateSlotDto> saveTemplate(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody TemplateRequest body) {
        List<TemplateSlot> slots = body.slots().stream()
                .map(s -> new TemplateSlot(DayOfWeek.of(s.weekday()), s.kind(), s.minutes())).toList();
        return tracking.saveTemplate(user.id(), slots).stream().map(TemplateSlotDto::from).toList();
    }

    @PutMapping(path = "/api/tracking/review-habit", consumes = MediaType.APPLICATION_JSON_VALUE)
    HabitResponse saveHabit(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody HabitRequest body) {
        String note = body.note() == null || body.note().isBlank() ? null : body.note().trim();
        return HabitResponse.from(tracking.saveHabit(user.id(),
                new ReviewHabit(body.day(), body.readDiff(), body.explained(), body.tested(), note)));
    }

    @ExceptionHandler({InvalidTrackingException.class, IllegalArgumentException.class})
    ResponseEntity<ProblemDetail> invalid(RuntimeException e) {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage()));
    }
}

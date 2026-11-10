package dev.rotadev.tracking.web;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.rotadev.identity.AuthenticatedUser;
import dev.rotadev.tracking.application.CheckinService;
import dev.rotadev.tracking.application.InvalidTrackingException;
import dev.rotadev.tracking.domain.MilestoneAchievement;
import dev.rotadev.tracking.domain.WeeklyCheckin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
class CheckinController {

    private final CheckinService checkins;

    CheckinController(CheckinService checkins) {
        this.checkins = checkins;
    }

    record CheckinRequest(@Min(0) @Max(1000) int commits, @Min(1) @Max(5) int energy, @Size(max = 2000) String blockers,
            @Size(max = 2000) String notes) {
    }

    record CheckinResponse(@NotNull LocalDate weekStart, @NotNull int minutesLogged, @NotNull int reviewDays, @NotNull int commits,
            @NotNull int energy, String blockers, String notes) {

        static CheckinResponse from(WeeklyCheckin c) {
            return new CheckinResponse(c.weekStart(), c.minutesLogged(), c.reviewDays(), c.commits(), c.energy(), c.blockers(), c.notes());
        }
    }

    /** @param openWeek segunda-feira da semana aberta para check-in, ou nulo no meio da semana */
    record CheckinStatusResponse(LocalDate openWeek, @NotNull int minutesLogged, @NotNull int reviewDays, CheckinResponse done) {
    }

    record MilestoneRequest(@NotBlank @Size(max = 300) String milestone, @NotBlank @Size(max = 500) String evidenceUrl) {
    }

    record MilestoneAchievementResponse(@NotNull UUID id, @NotNull int planVersion, @NotNull String milestone,
            @NotNull String evidenceUrl, @NotNull LocalDate achievedOn) {

        static MilestoneAchievementResponse from(MilestoneAchievement a) {
            return new MilestoneAchievementResponse(a.id(), a.planVersion(), a.milestone(), a.evidenceUrl(), a.achievedOn());
        }
    }

    @GetMapping("/api/checkin")
    CheckinStatusResponse status(@AuthenticationPrincipal AuthenticatedUser user) {
        var s = checkins.status(user.id());
        return new CheckinStatusResponse(s.openWeek(), s.minutesLogged(), s.reviewDays(), s.done() == null ? null : CheckinResponse.from(s.done()));
    }

    @PostMapping(path = "/api/checkin", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    CheckinResponse submit(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CheckinRequest body) {
        return CheckinResponse.from(checkins.submit(user.id(), body.commits(), body.energy(), body.blockers(), body.notes()));
    }

    @GetMapping("/api/checkin/history")
    List<CheckinResponse> history(@AuthenticationPrincipal AuthenticatedUser user) {
        return checkins.history(user.id()).stream().map(CheckinResponse::from).toList();
    }

    @GetMapping("/api/milestones")
    List<MilestoneAchievementResponse> achievements(@AuthenticationPrincipal AuthenticatedUser user) {
        return checkins.achievements(user.id()).stream().map(MilestoneAchievementResponse::from).toList();
    }

    @PostMapping(path = "/api/milestones", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    MilestoneAchievementResponse achieve(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody MilestoneRequest body) {
        return MilestoneAchievementResponse.from(checkins.achieve(user.id(), body.milestone().trim(), body.evidenceUrl().trim()));
    }

    @ExceptionHandler({InvalidTrackingException.class, IllegalArgumentException.class})
    ResponseEntity<ProblemDetail> invalid(RuntimeException e) {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage()));
    }
}

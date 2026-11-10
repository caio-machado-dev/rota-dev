package dev.rotadev.diagnosis.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import dev.rotadev.diagnosis.application.DiagnosisNotPossibleException;
import dev.rotadev.diagnosis.application.DiagnosisStore;
import dev.rotadev.diagnosis.application.DiagnosisStore.StoredDiagnosis;
import dev.rotadev.diagnosis.application.RequestDiagnosis;
import dev.rotadev.diagnosis.web.DiagnosisResponse.AxisResponse;
import dev.rotadev.diagnosis.web.DiagnosisResponse.CitedEvidence;
import dev.rotadev.diagnosis.web.DiagnosisResponse.GapResponse;
import dev.rotadev.identity.AuthenticatedUser;
import dev.rotadev.profile.Evidence;
import dev.rotadev.profile.ProfileReader;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping(path = "/api/diagnosis", produces = MediaType.APPLICATION_JSON_VALUE)
class DiagnosisController {

    private final RequestDiagnosis request;
    private final DiagnosisStore store;
    private final ProfileReader profile;

    DiagnosisController(RequestDiagnosis request, DiagnosisStore store, ProfileReader profile) {
        this.request = request;
        this.store = store;
        this.profile = profile;
    }

    record Created(@NotNull UUID id) {
    }

    /** 202: o diagnóstico vai ser gerado num job. O front consulta {@code /latest} até ficar READY. */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    Created create(@AuthenticationPrincipal AuthenticatedUser user) {
        return new Created(request.request(user.id()));
    }

    @GetMapping("/latest")
    DiagnosisResponse latest(@AuthenticationPrincipal AuthenticatedUser user) {
        return store.latest(user.id()).map(d -> toResponse(user, d))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{id}")
    DiagnosisResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return store.find(user.id(), id).map(d -> toResponse(user, d))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** 409 Conflict: o pedido é válido, mas o estado atual (sem objetivo ou sem perfil) não permite. */
    @ExceptionHandler(DiagnosisNotPossibleException.class)
    ResponseEntity<ProblemDetail> notPossible(DiagnosisNotPossibleException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage()));
    }

    private DiagnosisResponse toResponse(AuthenticatedUser user, StoredDiagnosis d) {
        if (d.result() == null) {
            return new DiagnosisResponse(d.id(), d.status(), d.createdAt(), null, null, List.of(), List.of(), d.error());
        }
        Map<UUID, Evidence> evidences = new HashMap<>();
        profile.latestPerSource(user.id()).forEach(s -> s.evidences().forEach(e -> evidences.put(e.id(), e)));

        var axes = d.result().axes().stream().map(a -> new AxisResponse(a.key(), a.name(), a.required(), a.score(), a.verified(), a.rationale(),
                a.evidenceIds().stream().map(id -> {
                    Evidence e = evidences.get(id);
                    return new CitedEvidence(id, e == null ? null : e.title(), e == null ? null : e.url());
                }).toList())).toList();
        var gaps = d.result().gaps().stream()
                .map(g -> new GapResponse(g.axisKey(), g.axisName(), g.size(), g.priority(), g.description())).toList();
        return new DiagnosisResponse(d.id(), d.status(), d.createdAt(), d.result().summary(), d.result().estimatedLevel(), axes, gaps, null);
    }
}

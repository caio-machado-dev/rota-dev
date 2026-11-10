package dev.rotadev.diagnosis.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.rotadev.diagnosis.DiagnosisReader;

/** Implementa a API pública traduzindo os tipos internos para os records de {@link DiagnosisReader}. */
@Service
class DiagnosisQueries implements DiagnosisReader {

    private final DiagnosisStore store;

    DiagnosisQueries(DiagnosisStore store) {
        this.store = store;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReadyDiagnosis> latestReady(UUID userId) {
        return store.latestReady(userId).map(d -> new ReadyDiagnosis(
                d.id(),
                d.result().estimatedLevel(),
                d.result().axes().stream().map(a -> new AxisScore(a.key(), a.name(), a.required(), a.score(), a.verified())).toList(),
                d.result().gaps().stream().map(g -> new GapItem(g.axisKey(), g.axisName(), g.size(), g.priority(), g.description())).toList()));
    }
}

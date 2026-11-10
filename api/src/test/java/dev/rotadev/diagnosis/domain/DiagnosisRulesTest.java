package dev.rotadev.diagnosis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class DiagnosisRulesTest {

    private static final UUID REPO = UUID.randomUUID();
    private final List<Axis> matrix = List.of(
            new Axis("language", "Linguagem principal", "", 3),
            new Axis("testing", "Testes", "", 3),
            new Axis("delivery", "CI/CD", "", 2));

    @Test
    void keepsOnlyRealEvidenceAndOrdersGapsByThePrize() {
        var ai = new AiDiagnosis("Resumo.", "JUNIOR", List.of(
                new AiDiagnosis.AxisAnswer("language", 3, List.of(REPO.toString()), "Java em 3 repositórios"),
                new AiDiagnosis.AxisAnswer("testing", 1, List.of(REPO.toString()), "Poucos testes"),
                new AiDiagnosis.AxisAnswer("delivery", 1, List.of(REPO.toString()), "Um workflow")),
                List.of(new AiDiagnosis.GapAnswer("testing", "Escrever testes de integração")));

        var result = DiagnosisRules.reconcile(matrix, Set.of(REPO), ai);

        assertThat(result.axes()).allSatisfy(a -> assertThat(a.verified()).isTrue());
        assertThat(result.gaps()).extracting(Gap::axisKey).containsExactly("testing", "delivery");
        assertThat(result.gaps().getFirst().description()).isEqualTo("Escrever testes de integração");
        assertThat(result.gaps().get(1).description()).isEqualTo("Subir de 1 para 2.");
    }

    @Test
    void inventedEvidenceMakesAxisUnverifiedAndCapped() {
        var ai = new AiDiagnosis("", "SENIOR", List.of(
                new AiDiagnosis.AxisAnswer("language", 5, List.of(UUID.randomUUID().toString(), "nao-e-uuid"), "Excelente")), List.of());

        var language = DiagnosisRules.reconcile(matrix, Set.of(REPO), ai).axes().getFirst();

        assertThat(language.verified()).isFalse();
        assertThat(language.evidenceIds()).isEmpty();
        assertThat(language.score()).isEqualTo(DiagnosisRules.UNVERIFIED_CAP);
    }

    @Test
    void missingAxisCountsAsZeroAndInventedAxisIsIgnored() {
        var ai = new AiDiagnosis("", "ROCKSTAR", List.of(new AiDiagnosis.AxisAnswer("blockchain", 5, List.of(), "")), List.of());

        var result = DiagnosisRules.reconcile(matrix, Set.of(), ai);

        assertThat(result.axes()).extracting(AxisAssessment::key).containsExactly("language", "testing", "delivery");
        assertThat(result.axes()).allSatisfy(a -> assertThat(a.score()).isZero());
        assertThat(result.estimatedLevel()).isEqualTo("NAO_ESTIMADO");
    }

    // TODO(caio): escrever o teste de "nota fora da escala é limitada a 0..5".
    //
    // O que fazer:
    //   1. Monte uma AiDiagnosis com o eixo "language" com score 9 e outro com score -2, ambos
    //      citando REPO (para ficarem verificados e o teto de não verificado não interferir).
    //   2. Rode DiagnosisRules.reconcile(matrix, Set.of(REPO), ai).
    //   3. Confira que as notas viraram 5 e 0. Dica: a regra está no Math.clamp de DiagnosisRules.
    //
    // Critério de pronto: `./mvnw test -Dtest=DiagnosisRulesTest` passa com 4 testes, nenhum pulado.
    @Disabled("TODO(caio): exercício da Fase 2")
    @Test
    void scoresOutsideTheScaleAreClamped() {
    }
}

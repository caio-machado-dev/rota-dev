package dev.rotadev.diagnosis.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Confere a resposta da IA contra o que sabemos ser verdade. A IA sugere; quem decide é esta regra.
 *
 * <ul>
 *   <li>Só valem eixos da matriz da trilha: um eixo inventado é ignorado, um que faltou entra com nota 0.</li>
 *   <li>Só valem evidências que existem no perfil do usuário. Um id inventado é descartado.</li>
 *   <li><b>Sem evidência, não há afirmação:</b> eixo sem nenhuma evidência válida fica "não verificado"
 *       e com nota no máximo {@value #UNVERIFIED_CAP}.</li>
 *   <li>A lacuna é {@code exigida - atual}, calculada aqui, e as prioridades saem da maior lacuna.</li>
 * </ul>
 */
public final class DiagnosisRules {

    public static final int UNVERIFIED_CAP = 1;
    private static final Set<String> LEVELS = Set.of("JUNIOR", "PLENO", "SENIOR", "ESPECIALISTA");

    private DiagnosisRules() {
    }

    public static DiagnosisResult reconcile(List<Axis> matrix, Set<UUID> knownEvidence, AiDiagnosis ai) {
        Map<String, AiDiagnosis.AxisAnswer> answers = ai.axes().stream()
                .collect(Collectors.toMap(AiDiagnosis.AxisAnswer::key, Function.identity(), (a, b) -> a));
        Map<String, String> gapText = ai.gaps().stream()
                .collect(Collectors.toMap(AiDiagnosis.GapAnswer::axisKey, AiDiagnosis.GapAnswer::description, (a, b) -> a));

        List<AxisAssessment> axes = new ArrayList<>();
        for (Axis axis : matrix) {
            AiDiagnosis.AxisAnswer answer = answers.get(axis.key());
            if (answer == null) {
                axes.add(new AxisAssessment(axis.key(), axis.name(), axis.required(), 0, false, List.of(), "Sem dados sobre este eixo."));
                continue;
            }
            List<UUID> evidence = answer.evidenceIds().stream()
                    .map(DiagnosisRules::parseUuid)
                    .filter(id -> id != null && knownEvidence.contains(id))
                    .distinct()
                    .toList();
            boolean verified = !evidence.isEmpty();
            int score = Math.clamp(answer.score(), 0, 5);
            if (!verified) {
                score = Math.min(score, UNVERIFIED_CAP);
            }
            axes.add(new AxisAssessment(axis.key(), axis.name(), axis.required(), score, verified, evidence, answer.rationale()));
        }

        List<AxisAssessment> withGap = axes.stream()
                .filter(a -> a.gap() > 0)
                .sorted(Comparator.comparingInt(AxisAssessment::gap).reversed()
                        .thenComparing(Comparator.comparingInt(AxisAssessment::required).reversed()))
                .toList();
        List<Gap> gaps = new ArrayList<>();
        for (int i = 0; i < withGap.size(); i++) {
            AxisAssessment a = withGap.get(i);
            String description = gapText.getOrDefault(a.key(), "Subir de " + a.score() + " para " + a.required() + ".");
            gaps.add(new Gap(a.key(), a.name(), a.gap(), i + 1, description));
        }

        String level = ai.estimatedLevel() != null && LEVELS.contains(ai.estimatedLevel()) ? ai.estimatedLevel() : "NAO_ESTIMADO";
        return new DiagnosisResult(ai.summary(), level, axes, gaps);
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}

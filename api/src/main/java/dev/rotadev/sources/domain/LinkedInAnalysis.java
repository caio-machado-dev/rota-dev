package dev.rotadev.sources.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.rotadev.profile.Confidence;
import dev.rotadev.profile.Evidence;
import dev.rotadev.profile.SkillSignal;

/**
 * Regra pura para o export do LinkedIn:
 * <ul>
 *   <li>cada cargo vira uma evidência {@code EXPERIENCE} de confiança {@code MEDIUM} (fonte estruturada);</li>
 *   <li>cada certificação vira uma evidência {@code CERTIFICATION}, também {@code MEDIUM};</li>
 *   <li>tecnologias citadas nas descrições dos cargos sobem para {@code MEDIUM}; as que só estão na
 *       lista de competências ficam {@code LOW} (declaradas, sem contexto).</li>
 * </ul>
 */
public final class LinkedInAnalysis {

    private LinkedInAnalysis() {
    }

    public record Result(List<SkillSignal> skills, List<Evidence> evidences) {
    }

    public static Result analyze(LinkedInData data) {
        List<Evidence> evidences = new ArrayList<>();
        Map<String, Confidence> confidence = new LinkedHashMap<>();
        Map<String, Integer> mentions = new LinkedHashMap<>();

        for (LinkedInData.Position p : data.positions()) {
            String period = p.startedOn() + " – " + (p.finishedOn() == null || p.finishedOn().isBlank() ? "atual" : p.finishedOn());
            String text = p.title() + " " + nullToEmpty(p.description());
            var found = TextSkillMatcher.match(text, "LinkedIn").skills();
            String mainSkill = found.isEmpty() ? null : found.getFirst().name();
            evidences.add(new Evidence(null, "EXPERIENCE", mainSkill, p.title() + " · " + p.company(), period, null, Confidence.MEDIUM));
            for (SkillSignal s : found) {
                confidence.put(s.name(), Confidence.MEDIUM);
                mentions.merge(s.name(), 2, Integer::sum);
            }
        }
        for (String skill : data.skills()) {
            String name = canonical(skill);
            confidence.putIfAbsent(name, Confidence.LOW);
            mentions.merge(name, 1, Integer::sum);
        }
        for (LinkedInData.Certification c : data.certifications()) {
            evidences.add(new Evidence(null, "CERTIFICATION", null, c.name(), c.authority(), null, Confidence.MEDIUM));
        }

        int total = mentions.values().stream().mapToInt(Integer::intValue).sum();
        List<SkillSignal> skills = mentions.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(e -> new SkillSignal(e.getKey(), Math.round(1000.0 * e.getValue() / total) / 1000.0, confidence.get(e.getKey())))
                .toList();
        return new Result(skills, evidences);
    }

    /** "spring boot" na lista de competências vira "Spring", o mesmo nome do dicionário. */
    private static String canonical(String skill) {
        var found = TextSkillMatcher.match(skill, "LinkedIn").skills();
        return found.size() == 1 ? found.getFirst().name() : skill.trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

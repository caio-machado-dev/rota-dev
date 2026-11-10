package dev.rotadev.sources.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.rotadev.profile.Confidence;
import dev.rotadev.profile.Evidence;
import dev.rotadev.profile.SkillSignal;

/**
 * Regra pura: transforma os repositórios do GitHub em habilidades e evidências.
 * Sem Spring, sem HTTP, sem banco; por isso o teste dela é só chamar o método.
 *
 * <ul>
 *   <li>Forks e repositórios arquivados não contam: não mostram o que o usuário escreve hoje.</li>
 *   <li>O peso de uma linguagem é a fatia dela no total de bytes dos repositórios que contam.</li>
 *   <li>Confiança {@code HIGH} se a linguagem aparece em 2 repositórios ou mais; {@code MEDIUM} se em um só.</li>
 *   <li>Cada repositório vira uma evidência citável, com link.</li>
 * </ul>
 */
public final class GithubProfileAnalysis {

    static final int MAX_SKILLS = 10;

    private GithubProfileAnalysis() {
    }

    public record Result(List<SkillSignal> skills, List<Evidence> evidences) {
    }

    public static Result analyze(List<GithubRepo> repos) {
        List<GithubRepo> counted = repos.stream().filter(r -> !r.fork() && !r.archived()).toList();

        Map<String, Long> bytesPerLanguage = new HashMap<>();
        Map<String, Integer> reposPerLanguage = new HashMap<>();
        for (GithubRepo repo : counted) {
            repo.languages().forEach((language, bytes) -> {
                bytesPerLanguage.merge(language, bytes, Long::sum);
                reposPerLanguage.merge(language, 1, Integer::sum);
            });
        }
        long total = bytesPerLanguage.values().stream().mapToLong(Long::longValue).sum();

        List<SkillSignal> skills = bytesPerLanguage.entrySet().stream()
                // Maior peso primeiro; empate em ordem alfabética, para o resultado não depender
                // da ordem interna do HashMap.
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(MAX_SKILLS)
                .map(e -> new SkillSignal(
                        e.getKey(),
                        round((double) e.getValue() / total),
                        reposPerLanguage.get(e.getKey()) >= 2 ? Confidence.HIGH : Confidence.MEDIUM))
                .toList();

        List<Evidence> evidences = new ArrayList<>();
        for (GithubRepo repo : counted) {
            evidences.add(new Evidence(null, "REPOSITORY", mainLanguage(repo), repo.name(), describe(repo), repo.htmlUrl(), Confidence.HIGH));
        }
        return new Result(skills, evidences);
    }

    private static String mainLanguage(GithubRepo repo) {
        return repo.languages().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /** Ex.: "Java 72% · Python 28% · CI com GitHub Actions". */
    static String describe(GithubRepo repo) {
        long total = repo.languages().values().stream().mapToLong(Long::longValue).sum();
        String languages = repo.languages().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(3)
                .map(e -> e.getKey() + " " + Math.round(100.0 * e.getValue() / total) + "%")
                .collect(Collectors.joining(" · "));
        String ci = repo.hasCi() ? "CI com GitHub Actions" : "sem CI";
        return languages.isEmpty() ? ci : languages + " · " + ci;
    }

    private static double round(double value) {
        return Math.round(value * 1000) / 1000.0;
    }
}

package dev.rotadev.sources.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.rotadev.profile.Confidence;
import dev.rotadev.profile.Evidence;
import dev.rotadev.profile.SkillSignal;

/**
 * Acha tecnologias citadas num texto livre (currículo, perfil em PDF) por um dicionário fixo.
 *
 * <p>É de propósito simples e conservador: o que está escrito num currículo é <b>declarado</b>, não
 * provado, então tudo sai com confiança {@code LOW}. A Fase 2 usa IA para ler o texto com mais
 * contexto; esta regra garante um mínimo sem depender de IA.
 */
public final class TextSkillMatcher {

    /**
     * Nome exibido → expressão regular. As bordas {@code (?<![\w.#+-])} e {@code (?![\w#+])}
     * impedem que "Java" case dentro de "JavaScript" e que "Go" case dentro de "Google".
     */
    static final Map<String, Pattern> DICTIONARY = dictionary(
            "Java", "java",
            "JavaScript", "javascript|js",
            "TypeScript", "typescript|ts",
            "Python", "python",
            "Go", "golang",
            "Kotlin", "kotlin",
            "C#", "c#|\\.net",
            "Rust", "rust",
            "PHP", "php",
            "Ruby", "ruby|rails",
            "SQL", "sql",
            "PostgreSQL", "postgres|postgresql",
            "MySQL", "mysql",
            "MongoDB", "mongodb|mongo",
            "Redis", "redis",
            "Spring", "spring boot|spring",
            "Django", "django",
            "FastAPI", "fastapi",
            "Flask", "flask",
            "React", "react|react\\.js",
            "Next.js", "next\\.js|nextjs",
            "Node.js", "node\\.js|nodejs|node",
            "Docker", "docker",
            "Kubernetes", "kubernetes|k8s",
            "AWS", "aws|amazon web services",
            "GCP", "gcp|google cloud",
            "Azure", "azure",
            "Terraform", "terraform",
            "Kafka", "kafka",
            "RabbitMQ", "rabbitmq",
            "GraphQL", "graphql",
            "Linux", "linux",
            "CI/CD", "ci/cd|github actions|gitlab ci|jenkins");

    private TextSkillMatcher() {
    }

    public record Result(List<SkillSignal> skills, List<Evidence> evidences) {
    }

    /** @param sourceLabel como a fonte aparece na evidência, ex.: "currículo" */
    public static Result match(String text, String sourceLabel) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> firstSnippet = new LinkedHashMap<>();
        DICTIONARY.forEach((name, pattern) -> {
            Matcher m = pattern.matcher(text);
            int count = 0;
            while (m.find()) {
                if (count == 0) {
                    firstSnippet.put(name, snippet(text, m.start(), m.end()));
                }
                count++;
            }
            if (count > 0) {
                counts.put(name, count);
            }
        });

        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        List<SkillSignal> skills = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(e -> new SkillSignal(e.getKey(), Math.round(1000.0 * e.getValue() / total) / 1000.0, Confidence.LOW))
                .toList();
        List<Evidence> evidences = new ArrayList<>();
        for (SkillSignal skill : skills) {
            evidences.add(new Evidence(null, "DECLARED", skill.name(), "Citado no " + sourceLabel,
                    "…" + firstSnippet.get(skill.name()) + "…", null, Confidence.LOW));
        }
        return new Result(skills, evidences);
    }

    /** Uns 40 caracteres de cada lado, numa linha só, para a evidência mostrar o contexto. */
    private static String snippet(String text, int start, int end) {
        int from = Math.max(0, start - 40);
        int to = Math.min(text.length(), end + 40);
        return text.substring(from, to).replaceAll("\\s+", " ").trim();
    }

    private static Map<String, Pattern> dictionary(String... pairs) {
        Map<String, Pattern> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], Pattern.compile("(?<![\\w.#+-])(?:" + pairs[i + 1] + ")(?![\\w#+])",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        }
        return map;
    }
}

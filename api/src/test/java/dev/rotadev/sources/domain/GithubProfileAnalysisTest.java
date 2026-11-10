package dev.rotadev.sources.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import dev.rotadev.profile.Confidence;
import dev.rotadev.profile.SkillSignal;

class GithubProfileAnalysisTest {

    @Test
    void weightsLanguagesByBytesAndRaisesConfidenceWhenRepeated() {
        var result = GithubProfileAnalysis.analyze(List.of(
                repo("api", Map.of("Java", 700L, "Shell", 100L), true),
                repo("cli", Map.of("Java", 100L, "Python", 100L), false)));

        assertThat(result.skills()).containsExactly(
                new SkillSignal("Java", 0.8, Confidence.HIGH),
                new SkillSignal("Python", 0.1, Confidence.MEDIUM),
                new SkillSignal("Shell", 0.1, Confidence.MEDIUM));
    }

    @Test
    void eachRepoBecomesACitableEvidence() {
        var result = GithubProfileAnalysis.analyze(List.of(repo("api", Map.of("Java", 720L, "Python", 280L), true)));

        assertThat(result.evidences()).singleElement().satisfies(e -> {
            assertThat(e.kind()).isEqualTo("REPOSITORY");
            assertThat(e.skill()).isEqualTo("Java");
            assertThat(e.detail()).isEqualTo("Java 72% · Python 28% · CI com GitHub Actions");
            assertThat(e.url()).isEqualTo("https://github.com/caio/api");
        });
    }

    @Test
    void noReposMeansNoSkills() {
        var result = GithubProfileAnalysis.analyze(List.of());

        assertThat(result.skills()).isEmpty();
        assertThat(result.evidences()).isEmpty();
    }

    // TODO(caio): escrever o teste da regra "forks e repositórios arquivados não contam".
    //
    // O que fazer:
    //   1. Monte três repositórios: um normal, um com fork=true e um com archived=true. Use o
    //      construtor de GithubRepo direto (o helper repo(...) abaixo sempre cria fork=false, archived=false).
    //   2. Chame GithubProfileAnalysis.analyze(...).
    //   3. Confira que só o repositório normal virou evidência e que as linguagens do fork
    //      e do arquivado não aparecem em result.skills().
    //
    // Critério de pronto: `./mvnw test -Dtest=GithubProfileAnalysisTest` passa com 4 testes, nenhum pulado.
    @Disabled("TODO(caio): exercício da Fase 1")
    @Test
    void forksAndArchivedReposAreIgnored() {
    }

    private static GithubRepo repo(String name, Map<String, Long> languages, boolean hasCi) {
        return new GithubRepo(name, "caio/" + name, "https://github.com/caio/" + name, null, false, false, 0,
                Instant.parse("2026-11-01T00:00:00Z"), languages, hasCi);
    }
}

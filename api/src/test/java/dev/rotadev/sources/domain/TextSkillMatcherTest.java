package dev.rotadev.sources.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.rotadev.profile.Confidence;
import dev.rotadev.profile.SkillSignal;

class TextSkillMatcherTest {

    @Test
    void findsDeclaredTechnologiesWithLowConfidence() {
        var result = TextSkillMatcher.match("Backend com Python e Django. Hoje estudo Java e Spring Boot. Python de novo.", "currículo");

        assertThat(result.skills()).extracting(SkillSignal::name).containsExactly("Python", "Django", "Java", "Spring");
        assertThat(result.skills()).allSatisfy(s -> assertThat(s.confidence()).isEqualTo(Confidence.LOW));
        assertThat(result.evidences().getFirst().title()).isEqualTo("Citado no currículo");
        assertThat(result.evidences().getFirst().detail()).contains("Python e Django");
    }

    @Test
    void javaDoesNotMatchInsideJavaScriptNorGoInsideGoogle() {
        var result = TextSkillMatcher.match("Front em JavaScript, deploy no Google Cloud.", "currículo");

        assertThat(result.skills()).extracting(SkillSignal::name).containsExactlyInAnyOrder("JavaScript", "GCP");
    }

    @Test
    void emptyTextGivesNothing() {
        assertThat(TextSkillMatcher.match("", "currículo").skills()).isEmpty();
    }
}
